package io.github.acarolinebcosta.docfy.document.application;

public record CreateDocumentCommand(
        String title,
        String description
) {

    public CreateDocumentCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Document title must not be blank"
            );
        }

        if (title.length() > 255) {
            throw new IllegalArgumentException(
                    "Document title must not exceed 255 characters"
            );
        }
    }
}
