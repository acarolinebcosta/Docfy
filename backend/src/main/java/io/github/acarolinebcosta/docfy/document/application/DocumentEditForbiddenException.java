package io.github.acarolinebcosta.docfy.document.application;

public class DocumentEditForbiddenException
        extends RuntimeException {

    public DocumentEditForbiddenException() {
        super("Document editing is not allowed");
    }
}