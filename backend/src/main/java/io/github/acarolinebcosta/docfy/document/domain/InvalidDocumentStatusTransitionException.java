package io.github.acarolinebcosta.docfy.document.domain;

public class InvalidDocumentStatusTransitionException
        extends RuntimeException {

    public InvalidDocumentStatusTransitionException(
            DocumentStatus currentStatus,
            DocumentStatus targetStatus
    ) {
        super(
                "Invalid document status transition from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}