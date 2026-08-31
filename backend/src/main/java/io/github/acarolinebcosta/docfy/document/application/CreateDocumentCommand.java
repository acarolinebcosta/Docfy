package io.github.acarolinebcosta.docfy.document.application;

import java.util.UUID;

public record CreateDocumentCommand(
        String title,
        String description,
        UUID categoryId
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

        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Document category must not be null"
            );
        }
    }
}
