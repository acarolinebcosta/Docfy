package io.github.acarolinebcosta.docfy.document.domain;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.category.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(
            name = "document_code",
            nullable = false,
            updatable = false,
            insertable = false,
            length = 20
    )
    private String documentCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {
    }

    public Document(
            String title,
            String description,
            User createdBy
    ) {
        this(
                title,
                description,
                createdBy,
                Category.otherReference()
        );
    }

    public Document(
            String title,
            String description,
            User createdBy,
            Category category
    ) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.category = category;
        this.status = DocumentStatus.DRAFT;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void updateMetadata(
            String title,
            boolean updateTitle,
            String description,
            boolean updateDescription,
            Category category,
            boolean updateCategory
    ) {
        if (updateTitle) {
            this.title = title;
        }

        if (updateDescription) {
            this.description = description;
        }

        if (updateCategory) {
            this.category = category;
        }
    }

    public void submitForReview() {
        transitionTo(
                DocumentStatus.DRAFT,
                DocumentStatus.IN_REVIEW
        );
    }

    public void approve() {
        transitionTo(
                DocumentStatus.IN_REVIEW,
                DocumentStatus.APPROVED
        );
    }

    public void reject() {
        transitionTo(
                DocumentStatus.IN_REVIEW,
                DocumentStatus.DRAFT
        );
    }

    public void archive() {
        transitionTo(
                DocumentStatus.APPROVED,
                DocumentStatus.ARCHIVED
        );
    }

    private void transitionTo(
            DocumentStatus expectedStatus,
            DocumentStatus targetStatus
    ) {
        if (status != expectedStatus) {
            throw new InvalidDocumentStatusTransitionException(
                    status,
                    targetStatus
            );
        }

        status = targetStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public String getDescription() {
        return description;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
