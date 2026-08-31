package io.github.acarolinebcosta.docfy.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    public static final UUID OTHER_CATEGORY_ID = UUID.fromString(
            "11111111-0000-0000-0000-000000000007"
    );

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Category() {
    }

    public Category(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Category otherReference() {
        return new Category(OTHER_CATEGORY_ID, "Other");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
