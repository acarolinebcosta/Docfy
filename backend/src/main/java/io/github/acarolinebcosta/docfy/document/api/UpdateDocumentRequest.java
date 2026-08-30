package io.github.acarolinebcosta.docfy.document.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public class UpdateDocumentRequest {

    private String title;
    private String description;

    private boolean titleProvided;
    private boolean descriptionProvided;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTitleProvided() {
        return titleProvided;
    }

    public boolean isDescriptionProvided() {
        return descriptionProvided;
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

    public boolean hasAnyFieldProvided() {
        return titleProvided || descriptionProvided;
    }
}