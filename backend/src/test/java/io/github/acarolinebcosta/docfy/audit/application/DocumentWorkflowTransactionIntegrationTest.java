package io.github.acarolinebcosta.docfy.audit.application;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditAction;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.application.DocumentWorkflowService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DocumentWorkflowTransactionIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private DocumentWorkflowService workflowService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private DocumentAuditService auditService;

    private UUID documentId;
    private UUID managerId;
    private UUID ownerId;

    @AfterEach
    void cleanUp() {
        if (documentId != null) {
            documentRepository.deleteById(documentId);
        }

        if (ownerId != null) {
            userRepository.deleteById(ownerId);
        }

        if (managerId != null) {
            userRepository.deleteById(managerId);
        }
    }

    @Test
    void shouldRollbackDocumentTransitionWhenAuditRecordingFails() {
        User manager = userRepository.save(
                new User(
                        "rollback-manager-"
                                + UUID.randomUUID()
                                + "@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.MANAGER
                )
        );

        managerId = manager.getId();

        User owner = userRepository.save(
                new User(
                        "rollback-owner-"
                                + UUID.randomUUID()
                                + "@docfy.local",
                        "{bcrypt}encoded-password",
                        Role.COLLABORATOR
                )
        );

        ownerId = owner.getId();

        Document document =
                documentRepository.saveAndFlush(
                        new Document(
                                "Transactional audit",
                                "Document used to validate rollback",
                                owner
                        )
                );

        documentId = document.getId();

        doThrow(
                new IllegalStateException(
                        "Simulated audit persistence failure"
                )
        )
                .when(auditService)
                .record(
                        any(Document.class),
                        eq(manager),
                        eq(DocumentAuditAction.DOCUMENT_SUBMITTED),
                        eq(DocumentStatus.DRAFT)
                );

        assertThrows(
                IllegalStateException.class,
                () -> workflowService.submit(
                        document.getId(),
                        manager
                )
        );

        Document persisted =
                documentRepository
                        .findById(document.getId())
                        .orElseThrow();

        assertEquals(
                DocumentStatus.DRAFT,
                persisted.getStatus()
        );

        verify(auditService).record(
                any(Document.class),
                eq(manager),
                eq(DocumentAuditAction.DOCUMENT_SUBMITTED),
                eq(DocumentStatus.DRAFT)
        );
    }
}