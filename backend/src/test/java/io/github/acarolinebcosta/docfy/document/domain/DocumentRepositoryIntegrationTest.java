package io.github.acarolinebcosta.docfy.document.domain;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DocumentRepositoryIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

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
}
