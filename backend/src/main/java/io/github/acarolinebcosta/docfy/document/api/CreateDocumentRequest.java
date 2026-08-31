package io.github.acarolinebcosta.docfy.document.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDocumentRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        @NotNull
        UUID categoryId
) {
}
