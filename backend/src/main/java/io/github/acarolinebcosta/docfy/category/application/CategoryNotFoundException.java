package io.github.acarolinebcosta.docfy.category.application;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found: " + categoryId);
    }
}
