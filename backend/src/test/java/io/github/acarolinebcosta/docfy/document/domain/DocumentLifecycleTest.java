package io.github.acarolinebcosta.docfy.document.domain;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentLifecycleTest {

    @Test
    void shouldSubmitDraftDocumentForReview() {
        Document document = documentInStatus(DocumentStatus.DRAFT);

        document.submitForReview();

        assertEquals(
                DocumentStatus.IN_REVIEW,
                document.getStatus()
        );
    }

    @Test
    void shouldApproveDocumentInReview() {
        Document document = documentInStatus(
                DocumentStatus.IN_REVIEW
        );

        document.approve();

        assertEquals(
                DocumentStatus.APPROVED,
                document.getStatus()
        );
    }

    @Test
    void shouldRejectDocumentInReviewBackToDraft() {
        Document document = documentInStatus(
                DocumentStatus.IN_REVIEW
        );

        document.reject();

        assertEquals(
                DocumentStatus.DRAFT,
                document.getStatus()
        );
    }

    @Test
    void shouldArchiveApprovedDocument() {
        Document document = documentInStatus(
                DocumentStatus.APPROVED
        );

        document.archive();

        assertEquals(
                DocumentStatus.ARCHIVED,
                document.getStatus()
        );
    }

    @Test
    void shouldAllowRejectedDocumentToBeSubmittedAgain() {
        Document document = documentInStatus(
                DocumentStatus.IN_REVIEW
        );

        document.reject();
        document.submitForReview();

        assertEquals(
                DocumentStatus.IN_REVIEW,
                document.getStatus()
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "IN_REVIEW",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldRejectSubmissionFromInvalidStatus(
            DocumentStatus initialStatus
    ) {
        Document document = documentInStatus(initialStatus);

        assertInvalidTransition(
                document,
                initialStatus,
                DocumentStatus.IN_REVIEW,
                document::submitForReview
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "DRAFT",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldRejectApprovalFromInvalidStatus(
            DocumentStatus initialStatus
    ) {
        Document document = documentInStatus(initialStatus);

        assertInvalidTransition(
                document,
                initialStatus,
                DocumentStatus.APPROVED,
                document::approve
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "DRAFT",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldRejectRejectionFromInvalidStatus(
            DocumentStatus initialStatus
    ) {
        Document document = documentInStatus(initialStatus);

        assertInvalidTransition(
                document,
                initialStatus,
                DocumentStatus.DRAFT,
                document::reject
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "DRAFT",
                    "IN_REVIEW",
                    "ARCHIVED"
            }
    )
    void shouldRejectArchiveFromInvalidStatus(
            DocumentStatus initialStatus
    ) {
        Document document = documentInStatus(initialStatus);

        assertInvalidTransition(
                document,
                initialStatus,
                DocumentStatus.ARCHIVED,
                document::archive
        );
    }

    private void assertInvalidTransition(
            Document document,
            DocumentStatus initialStatus,
            DocumentStatus targetStatus,
            Runnable transition
    ) {
        InvalidDocumentStatusTransitionException exception =
                assertThrows(
                        InvalidDocumentStatusTransitionException.class,
                        transition::run
                );

        assertEquals(
                "Invalid document status transition from "
                        + initialStatus
                        + " to "
                        + targetStatus,
                exception.getMessage()
        );

        assertEquals(
                initialStatus,
                document.getStatus()
        );
    }

    private Document documentInStatus(
            DocumentStatus status
    ) {
        User owner = new User(
                "owner@docfy.test",
                "password-hash",
                Role.COLLABORATOR
        );

        Document document = new Document(
                "Lifecycle document",
                "Lifecycle test",
                owner
        );

        switch (status) {
            case DRAFT -> {
                return document;
            }

            case IN_REVIEW -> document.submitForReview();

            case APPROVED -> {
                document.submitForReview();
                document.approve();
            }

            case ARCHIVED -> {
                document.submitForReview();
                document.approve();
                document.archive();
            }
        }

        return document;
    }
}