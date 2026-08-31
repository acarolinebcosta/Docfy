package io.github.acarolinebcosta.docfy.file.domain;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_files")
public class DocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private Document document;

    @Column(
            name = "original_filename",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "storage_key",
            nullable = false,
            unique = true,
            updatable = false,
            length = 255
    )
    private String storageKey;

    @Column(
            name = "content_type",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long size;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected DocumentFile() {
    }

    public DocumentFile(
            Document document,
            String originalFilename,
            String storageKey,
            String contentType,
            long size,
            User uploadedBy
    ) {
        this.document = document;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.size = size;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void prePersist() {
        uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
