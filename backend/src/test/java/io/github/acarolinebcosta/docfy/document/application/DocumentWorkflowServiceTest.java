package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.document.domain.InvalidDocumentStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentWorkflowServiceTest {

    private static final UUID DOCUMENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    @Test
    void shouldSubmitVisibleAuthorizedDraft() {
        Fixture fixture = fixture();

        when(fixture.repository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(fixture.document));
        when(
                fixture.visibilityPolicy.canView(
                        fixture.document,
                        fixture.actor
                )
        ).thenReturn(true);
        when(
                fixture.workflowPolicy.canSubmit(
                        fixture.document,
                        fixture.actor
                )
        ).thenReturn(true);

        Document result = fixture.service.submit(
                DOCUMENT_ID,
                fixture.actor
        );

        assertEquals(
                DocumentStatus.IN_REVIEW,
                result.getStatus()
        );

        verify(fixture.repository)
                .findById(DOCUMENT_ID);
        verify(fixture.visibilityPolicy)
                .canView(
                        fixture.document,
                        fixture.actor
                );
        verify(fixture.workflowPolicy)
                .canSubmit(
                        fixture.document,
                        fixture.actor
                );
    }

    @Test
    void shouldApproveVisibleAuthorizedDocumentInReview() {
        Fixture fixture = fixture();
        fixture.document.submitForReview();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canApprove(
                        fixture.actor
                )
        ).thenReturn(true);

        Document result = fixture.service.approve(
                DOCUMENT_ID,
                fixture.actor
        );

        assertEquals(
                DocumentStatus.APPROVED,
                result.getStatus()
        );
    }

    @Test
    void shouldRejectVisibleAuthorizedDocumentInReview() {
        Fixture fixture = fixture();
        fixture.document.submitForReview();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canReject(
                        fixture.actor
                )
        ).thenReturn(true);

        Document result = fixture.service.reject(
                DOCUMENT_ID,
                fixture.actor
        );

        assertEquals(
                DocumentStatus.DRAFT,
                result.getStatus()
        );
    }

    @Test
    void shouldArchiveVisibleAuthorizedApprovedDocument() {
        Fixture fixture = fixture();

        fixture.document.submitForReview();
        fixture.document.approve();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canArchive(
                        fixture.actor
                )
        ).thenReturn(true);

        Document result = fixture.service.archive(
                DOCUMENT_ID,
                fixture.actor
        );

        assertEquals(
                DocumentStatus.ARCHIVED,
                result.getStatus()
        );
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist() {
        Fixture fixture = fixture();

        when(fixture.repository.findById(DOCUMENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                DocumentNotFoundException.class,
                () -> fixture.service.submit(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        verifyNoInteractions(
                fixture.visibilityPolicy,
                fixture.workflowPolicy
        );
    }

    @Test
    void shouldConcealDocumentWhenActorCannotViewIt() {
        Fixture fixture = fixture();

        when(fixture.repository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(fixture.document));

        when(
                fixture.visibilityPolicy.canView(
                        fixture.document,
                        fixture.actor
                )
        ).thenReturn(false);

        assertThrows(
                DocumentNotFoundException.class,
                () -> fixture.service.submit(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        verifyNoInteractions(fixture.workflowPolicy);

        assertEquals(
                DocumentStatus.DRAFT,
                fixture.document.getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenActorCannotSubmit() {
        Fixture fixture = fixture();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canSubmit(
                        fixture.document,
                        fixture.actor
                )
        ).thenReturn(false);

        assertThrows(
                DocumentWorkflowForbiddenException.class,
                () -> fixture.service.submit(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        assertEquals(
                DocumentStatus.DRAFT,
                fixture.document.getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenActorCannotApprove() {
        Fixture fixture = fixture();
        fixture.document.submitForReview();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canApprove(
                        fixture.actor
                )
        ).thenReturn(false);

        assertThrows(
                DocumentWorkflowForbiddenException.class,
                () -> fixture.service.approve(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        assertEquals(
                DocumentStatus.IN_REVIEW,
                fixture.document.getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenActorCannotReject() {
        Fixture fixture = fixture();
        fixture.document.submitForReview();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canReject(
                        fixture.actor
                )
        ).thenReturn(false);

        assertThrows(
                DocumentWorkflowForbiddenException.class,
                () -> fixture.service.reject(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        assertEquals(
                DocumentStatus.IN_REVIEW,
                fixture.document.getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenActorCannotArchive() {
        Fixture fixture = fixture();

        fixture.document.submitForReview();
        fixture.document.approve();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canArchive(
                        fixture.actor
                )
        ).thenReturn(false);

        assertThrows(
                DocumentWorkflowForbiddenException.class,
                () -> fixture.service.archive(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        assertEquals(
                DocumentStatus.APPROVED,
                fixture.document.getStatus()
        );
    }

    @Test
    void shouldPropagateInvalidLifecycleTransition() {
        Fixture fixture = fixture();

        allowVisibleDocument(fixture);

        when(
                fixture.workflowPolicy.canApprove(
                        fixture.actor
                )
        ).thenReturn(true);

        assertThrows(
                InvalidDocumentStatusTransitionException.class,
                () -> fixture.service.approve(
                        DOCUMENT_ID,
                        fixture.actor
                )
        );

        assertEquals(
                DocumentStatus.DRAFT,
                fixture.document.getStatus()
        );
    }

    private void allowVisibleDocument(Fixture fixture) {
        when(fixture.repository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(fixture.document));

        when(
                fixture.visibilityPolicy.canView(
                        fixture.document,
                        fixture.actor
                )
        ).thenReturn(true);
    }

    private Fixture fixture() {
        DocumentRepository repository =
                mock(DocumentRepository.class);

        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);

        DocumentWorkflowPolicy workflowPolicy =
                mock(DocumentWorkflowPolicy.class);

        DocumentWorkflowService service =
                new DocumentWorkflowService(
                        repository,
                        visibilityPolicy,
                        workflowPolicy
                );

        User actor = new User(
                "actor@docfy.test",
                "password-hash",
                Role.MANAGER
        );

        Document document = new Document(
                "Workflow document",
                "Workflow service test",
                actor
        );

        return new Fixture(
                repository,
                visibilityPolicy,
                workflowPolicy,
                service,
                actor,
                document
        );
    }

    private record Fixture(
            DocumentRepository repository,
            DocumentVisibilityPolicy visibilityPolicy,
            DocumentWorkflowPolicy workflowPolicy,
            DocumentWorkflowService service,
            User actor,
            Document document
    ) {
    }
}