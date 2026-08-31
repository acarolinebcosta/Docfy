package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.category.api.CategoryResponse;
import io.github.acarolinebcosta.docfy.document.domain.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String documentCode,
        String title,
        String description,
        String status,
        CategoryResponse category,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getDocumentCode(),
                document.getTitle(),
                document.getDescription(),
                document.getStatus().name(),
                CategoryResponse.from(document.getCategory()),
                document.getCreatedBy().getId(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
