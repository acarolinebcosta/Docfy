package io.github.acarolinebcosta.docfy.audit.domain;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class DocumentAuditEventRepositoryIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private DocumentAuditEventRepository auditEventRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistDocumentAuditEvent() {
        User actor = saveManager();

        Document document = documentRepository.saveAndFlush(
                new Document(
                        "Audit persistence",
                        "Document used to validate audit persistence",
                        actor
                )
        );

        DocumentStatus previousStatus = document.getStatus();

        document.submitForReview();

        String correlationId = UUID.randomUUID().toString();

        DocumentAuditEvent saved =
                auditEventRepository.saveAndFlush(
                        new DocumentAuditEvent(
                                document,
                                actor,
                                DocumentAuditAction.DOCUMENT_SUBMITTED,
                                previousStatus,
                                document.getStatus(),
                                correlationId
                        )
                );

        assertNotNull(saved.getId());
        assertEquals(
                document.getId(),
                saved.getDocument().getId()
        );
        assertEquals(
                actor.getId(),
                saved.getActor().getId()
        );
        assertEquals(
                DocumentAuditAction.DOCUMENT_SUBMITTED,
                saved.getAction()
        );
        assertEquals(
                DocumentStatus.DRAFT,
                saved.getPreviousStatus()
        );
        assertEquals(
                DocumentStatus.IN_REVIEW,
                saved.getNewStatus()
        );
        assertEquals(
                correlationId,
                saved.getCorrelationId()
        );
        assertNotNull(saved.getOccurredAt());
    }

    @Test
    void shouldReturnOnlyAuditEventsForRequestedDocument() {
        User actor = saveManager();

        Document firstDocument = saveDocument(
                "First audited document",
                actor
        );

        Document secondDocument = saveDocument(
                "Second audited document",
                actor
        );

        createSubmissionAudit(firstDocument, actor);
        createSubmissionAudit(secondDocument, actor);

        List<DocumentAuditEvent> events =
                auditEventRepository
                        .findByDocumentIdOrderByOccurredAtAscIdAsc(
                                firstDocument.getId()
                        );

        assertEquals(1, events.size());
        assertEquals(
                firstDocument.getId(),
                events.getFirst().getDocument().getId()
        );
    }

    private User saveManager() {
        return userRepository.save(
                new User(
                        "audit-manager-"
                                + UUID.randomUUID()
                                + "@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.MANAGER
                )
        );
    }

    private Document saveDocument(
            String title,
            User creator
    ) {
        return documentRepository.saveAndFlush(
                new Document(
                        title,
                        "Audit repository integration test",
                        creator
                )
        );
    }

    private void createSubmissionAudit(
            Document document,
            User actor
    ) {
        DocumentStatus previousStatus = document.getStatus();

        document.submitForReview();

        auditEventRepository.saveAndFlush(
                new DocumentAuditEvent(
                        document,
                        actor,
                        DocumentAuditAction.DOCUMENT_SUBMITTED,
                        previousStatus,
                        document.getStatus(),
                        UUID.randomUUID().toString()
                )
        );
    }
}