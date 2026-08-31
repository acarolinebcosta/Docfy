package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;

import java.util.UUID;

public record DocumentListCriteria(
        String search,
        UUID categoryId,
        DocumentStatus status
) {

    public DocumentListCriteria {
        search = normalizeSearch(search);
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }
}
