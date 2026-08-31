package io.github.acarolinebcosta.docfy.document.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.UUID;

public class UpdateDocumentRequest {

    private String title;
    private String description;
    private UUID categoryId;

    private boolean titleProvided;
    private boolean descriptionProvided;
    private boolean categoryProvided;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public boolean isTitleProvided() {
        return titleProvided;
    }

    public boolean isDescriptionProvided() {
        return descriptionProvided;
    }

    public boolean isCategoryProvided() {
        return categoryProvided;
    }

    @JsonSetter(value = "title", nulls = Nulls.SET)
    public void setTitle(String title) {
        this.title = title;
        this.titleProvided = true;
    }

    @JsonSetter(value = "description", nulls = Nulls.SET)
    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }

    @JsonSetter(value = "categoryId", nulls = Nulls.SET)
    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
        this.categoryProvided = true;
    }

    public boolean hasAnyFieldProvided() {
        return titleProvided || descriptionProvided || categoryProvided;
    }
}
