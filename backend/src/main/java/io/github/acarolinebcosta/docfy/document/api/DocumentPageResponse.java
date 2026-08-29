package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.document.domain.Document;
import org.springframework.data.domain.Page;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static DocumentPageResponse from(Page<Document> documents) {
        return new DocumentPageResponse(
                documents.getContent()
                        .stream()
                        .map(DocumentResponse::from)
                        .toList(),
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements(),
                documents.getTotalPages()
        );
    }
}
