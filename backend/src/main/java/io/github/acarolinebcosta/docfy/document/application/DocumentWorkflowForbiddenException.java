package io.github.acarolinebcosta.docfy.document.application;

public class DocumentWorkflowForbiddenException
        extends RuntimeException {

    public DocumentWorkflowForbiddenException() {
        super("Document workflow action is not allowed");
    }
}