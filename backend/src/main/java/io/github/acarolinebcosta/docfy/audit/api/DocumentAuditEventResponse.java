package io.github.acarolinebcosta.docfy.audit.api;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditAction;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEvent;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentAuditEventResponse(
        UUID id,
        UUID documentId,
        UUID actorId,
        DocumentAuditAction action,
        DocumentStatus previousStatus,
        DocumentStatus newStatus,
        Instant occurredAt,
        String correlationId
) {

    public static DocumentAuditEventResponse from(
            DocumentAuditEvent event
    ) {
        return new DocumentAuditEventResponse(
                event.getId(),
                event.getDocument().getId(),
                event.getActor().getId(),
                event.getAction(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getOccurredAt(),
                event.getCorrelationId()
        );
    }
}