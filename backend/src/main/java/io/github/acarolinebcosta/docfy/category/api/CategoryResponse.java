package io.github.acarolinebcosta.docfy.category.api;

import io.github.acarolinebcosta.docfy.category.domain.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}
