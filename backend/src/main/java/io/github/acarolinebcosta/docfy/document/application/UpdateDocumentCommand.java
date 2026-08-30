package io.github.acarolinebcosta.docfy.document.application;

public record UpdateDocumentCommand(
        String title,
        boolean titleProvided,
        String description,
        boolean descriptionProvided
) {
}