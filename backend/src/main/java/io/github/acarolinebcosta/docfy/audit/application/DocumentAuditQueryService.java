package io.github.acarolinebcosta.docfy.audit.application;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEvent;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEventRepository;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.application.DocumentNotFoundException;
import io.github.acarolinebcosta.docfy.document.application.DocumentVisibilityPolicy;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentAuditQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentVisibilityPolicy visibilityPolicy;
    private final DocumentAuditPolicy auditPolicy;
    private final DocumentAuditEventRepository auditEventRepository;

    public DocumentAuditQueryService(
            DocumentRepository documentRepository,
            DocumentVisibilityPolicy visibilityPolicy,
            DocumentAuditPolicy auditPolicy,
            DocumentAuditEventRepository auditEventRepository
    ) {
        this.documentRepository = documentRepository;
        this.visibilityPolicy = visibilityPolicy;
        this.auditPolicy = auditPolicy;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public List<DocumentAuditEvent> history(
            UUID documentId,
            User actor
    ) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(
                        () -> new DocumentNotFoundException(
                                documentId
                        )
                );

        if (!visibilityPolicy.canView(document, actor)) {
            throw new DocumentNotFoundException(documentId);
        }

        if (!auditPolicy.canViewAudit(actor)) {
            throw new DocumentAuditForbiddenException();
        }

        return auditEventRepository
                .findByDocumentIdOrderByOccurredAtAscIdAsc(
                        documentId
                );
    }
}