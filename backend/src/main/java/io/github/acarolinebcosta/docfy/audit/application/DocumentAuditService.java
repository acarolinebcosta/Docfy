package io.github.acarolinebcosta.docfy.audit.application;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditAction;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEvent;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEventRepository;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdProvider;
import org.springframework.stereotype.Service;

@Service
public class DocumentAuditService {

    private final DocumentAuditEventRepository auditEventRepository;
    private final CorrelationIdProvider correlationIdProvider;

    public DocumentAuditService(
            DocumentAuditEventRepository auditEventRepository,
            CorrelationIdProvider correlationIdProvider
    ) {
        this.auditEventRepository = auditEventRepository;
        this.correlationIdProvider = correlationIdProvider;
    }

    public void record(
            Document document,
            User actor,
            DocumentAuditAction action,
            DocumentStatus previousStatus
    ) {
        DocumentAuditEvent event =
                new DocumentAuditEvent(
                        document,
                        actor,
                        action,
                        previousStatus,
                        document.getStatus(),
                        correlationIdProvider.currentCorrelationId()
                );

        auditEventRepository.save(event);
    }
}