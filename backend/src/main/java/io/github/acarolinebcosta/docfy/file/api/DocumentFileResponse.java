package io.github.acarolinebcosta.docfy.file.api;

import io.github.acarolinebcosta.docfy.file.domain.DocumentFile;

import java.time.Instant;
import java.util.UUID;

public record DocumentFileResponse(
        UUID id,
        UUID documentId,
        String originalFilename,
        String contentType,
        long size,
        UUID uploadedBy,
        Instant uploadedAt
) {

    public static DocumentFileResponse from(DocumentFile file) {
        return new DocumentFileResponse(
                file.getId(),
                file.getDocument().getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getUploadedBy().getId(),
                file.getUploadedAt()
        );
    }
}
