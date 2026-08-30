package io.github.acarolinebcosta.docfy.audit.domain;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "document_audit_events")
public class DocumentAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            updatable = false
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false,
            updatable = false
    )
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40,
            updatable = false
    )
    private DocumentAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "previous_status",
            nullable = false,
            length = 30,
            updatable = false
    )
    private DocumentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "new_status",
            nullable = false,
            length = 30,
            updatable = false
    )
    private DocumentStatus newStatus;

    @Column(
            name = "occurred_at",
            nullable = false,
            updatable = false
    )
    private Instant occurredAt;

    @Column(
            name = "correlation_id",
            length = 36,
            updatable = false
    )
    private String correlationId;

    protected DocumentAuditEvent() {
    }

    public DocumentAuditEvent(
            Document document,
            User actor,
            DocumentAuditAction action,
            DocumentStatus previousStatus,
            DocumentStatus newStatus,
            String correlationId
    ) {
        this.document = Objects.requireNonNull(document);
        this.actor = Objects.requireNonNull(actor);
        this.action = Objects.requireNonNull(action);
        this.previousStatus = Objects.requireNonNull(previousStatus);
        this.newStatus = Objects.requireNonNull(newStatus);

        if (previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Audit event must represent a status change"
            );
        }

        this.correlationId = correlationId;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public User getActor() {
        return actor;
    }

    public DocumentAuditAction getAction() {
        return action;
    }

    public DocumentStatus getPreviousStatus() {
        return previousStatus;
    }

    public DocumentStatus getNewStatus() {
        return newStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}