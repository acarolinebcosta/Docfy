package io.github.acarolinebcosta.docfy.document.application;

import java.util.UUID;

public record UpdateDocumentCommand(
        String title,
        boolean titleProvided,
        String description,
        boolean descriptionProvided,
        UUID categoryId,
        boolean categoryProvided
) {

    public UpdateDocumentCommand(
            String title,
            boolean titleProvided,
            String description,
            boolean descriptionProvided
    ) {
        this(
                title,
                titleProvided,
                description,
                descriptionProvided,
                null,
                false
        );
    }
}
