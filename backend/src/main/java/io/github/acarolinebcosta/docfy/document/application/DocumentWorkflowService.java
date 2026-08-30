package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentWorkflowService {

    private final DocumentRepository documentRepository;
    private final DocumentVisibilityPolicy visibilityPolicy;
    private final DocumentWorkflowPolicy workflowPolicy;

    public DocumentWorkflowService(
            DocumentRepository documentRepository,
            DocumentVisibilityPolicy visibilityPolicy,
            DocumentWorkflowPolicy workflowPolicy
    ) {
        this.documentRepository = documentRepository;
        this.visibilityPolicy = visibilityPolicy;
        this.workflowPolicy = workflowPolicy;
    }

    @Transactional
    public Document submit(
            UUID documentId,
            User actor
    ) {
        Document document = visibleDocument(
                documentId,
                actor
        );

        if (!workflowPolicy.canSubmit(document, actor)) {
            throw new DocumentWorkflowForbiddenException();
        }

        document.submitForReview();

        return document;
    }

    @Transactional
    public Document approve(
            UUID documentId,
            User actor
    ) {
        Document document = visibleDocument(
                documentId,
                actor
        );

        if (!workflowPolicy.canApprove(actor)) {
            throw new DocumentWorkflowForbiddenException();
        }

        document.approve();

        return document;
    }

    @Transactional
    public Document reject(
            UUID documentId,
            User actor
    ) {
        Document document = visibleDocument(
                documentId,
                actor
        );

        if (!workflowPolicy.canReject(actor)) {
            throw new DocumentWorkflowForbiddenException();
        }

        document.reject();

        return document;
    }

    @Transactional
    public Document archive(
            UUID documentId,
            User actor
    ) {
        Document document = visibleDocument(
                documentId,
                actor
        );

        if (!workflowPolicy.canArchive(actor)) {
            throw new DocumentWorkflowForbiddenException();
        }

        document.archive();

        return document;
    }

    private Document visibleDocument(
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

        return document;
    }
}