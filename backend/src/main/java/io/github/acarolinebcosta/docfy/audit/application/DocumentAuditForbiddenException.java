package io.github.acarolinebcosta.docfy.audit.application;

public class DocumentAuditForbiddenException
        extends RuntimeException {

    public DocumentAuditForbiddenException() {
        super("Document audit access is not allowed");
    }
}