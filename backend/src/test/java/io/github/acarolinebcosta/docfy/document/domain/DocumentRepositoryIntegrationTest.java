package io.github.acarolinebcosta.docfy.document.domain;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DocumentRepositoryIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistDocumentWithDraftStatus() {
        User user = userRepository.save(
                new User(
                        "document-owner@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.COLLABORATOR
                )
        );

        Document document = new Document(
                "Quality Strategy",
                "Initial quality strategy document",
                user
        );

        Document saved = documentRepository.saveAndFlush(document);

        assertNotNull(saved.getId());
        assertNotNull(saved.getDocumentCode());
        assertTrue(saved.getDocumentCode().matches("DOC-\\d{6,}"));
        assertEquals("Quality Strategy", saved.getTitle());
        assertEquals(
                "Initial quality strategy document",
                saved.getDescription()
        );
        assertEquals(DocumentStatus.DRAFT, saved.getStatus());
        assertEquals(user.getId(), saved.getCreatedBy().getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldLoadPersistedDocument() {
        User user = userRepository.save(
                new User(
                        "document-reader@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.MANAGER
                )
        );

        Document saved = documentRepository.saveAndFlush(
                new Document(
                        "Architecture",
                        "System architecture document",
                        user
                )
        );

        Document found = documentRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("Architecture", found.getTitle());
        assertEquals(DocumentStatus.DRAFT, found.getStatus());
        assertEquals(user.getId(), found.getCreatedBy().getId());
    }

    @Test
    void shouldRejectDocumentWithoutCreator() {
        Document document = new Document(
                "Document without owner",
                "Invalid document",
                null
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> documentRepository.saveAndFlush(document)
        );
    }

    @Test
    void shouldPaginateOnlyOwnOrApprovedDocumentsForCollaborator() {
        User viewer = userRepository.save(
                new User(
                        "repository-viewer@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.COLLABORATOR
                )
        );
        User otherUser = userRepository.save(
                new User(
                        "repository-other@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.COLLABORATOR
                )
        );

        Document ownDraft = saveDocument("Own draft", viewer);
        Document ownArchived = saveDocument("Own archived", viewer);
        Document otherApproved = saveDocument(
                "Other approved",
                otherUser
        );
        Document otherDraft = saveDocument("Other draft", otherUser);

        updateStatus(ownArchived, DocumentStatus.ARCHIVED);
        updateStatus(otherApproved, DocumentStatus.APPROVED);
        entityManager.clear();

        Sort sort = Sort.by(
                Sort.Order.desc("updatedAt"),
                Sort.Order.asc("id")
        );

        Page<Document> firstPage =
                documentRepository.findByCreatedByIdOrStatus(
                        viewer.getId(),
                        DocumentStatus.APPROVED,
                        PageRequest.of(0, 2, sort)
                );
        Page<Document> secondPage =
                documentRepository.findByCreatedByIdOrStatus(
                        viewer.getId(),
                        DocumentStatus.APPROVED,
                        PageRequest.of(1, 2, sort)
                );

        Set<UUID> visibleIds = Stream.concat(
                        firstPage.getContent().stream(),
                        secondPage.getContent().stream()
                )
                .map(Document::getId)
                .collect(Collectors.toSet());

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, firstPage.getNumberOfElements());
        assertEquals(1, secondPage.getNumberOfElements());
        assertEquals(
                Set.of(
                        ownDraft.getId(),
                        ownArchived.getId(),
                        otherApproved.getId()
                ),
                visibleIds
        );
        assertFalse(visibleIds.contains(otherDraft.getId()));
    }

    private Document saveDocument(String title, User creator) {
        return documentRepository.saveAndFlush(
                new Document(
                        title,
                        "Repository visibility test",
                        creator
                )
        );
    }

    private void updateStatus(
            Document document,
            DocumentStatus status
    ) {
        jdbcTemplate.update(
                "UPDATE documents SET status = ? WHERE id = ?",
                status.name(),
                document.getId()
        );
    }
}
