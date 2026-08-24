package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.document.domain.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String description,
        String status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getStatus().name(),
                document.getCreatedBy().getId(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
