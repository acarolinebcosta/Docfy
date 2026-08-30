package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentApplicationService {

    private final DocumentRepository documentRepository;
    private final DocumentVisibilityPolicy visibilityPolicy;
    private final DocumentEditPolicy editPolicy;

    public DocumentApplicationService(
            DocumentRepository documentRepository,
            DocumentVisibilityPolicy visibilityPolicy,
            DocumentEditPolicy editPolicy
    ) {
        this.documentRepository = documentRepository;
        this.visibilityPolicy = visibilityPolicy;
        this.editPolicy = editPolicy;
    }

    public Document create(
            CreateDocumentCommand command,
            User createdBy
    ) {
        Document document = new Document(
                command.title(),
                command.description(),
                createdBy
        );

        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public Document getById(
            UUID documentId,
            User viewer
    ) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(
                        () -> new DocumentNotFoundException(documentId)
                );

        if (!visibilityPolicy.canView(document, viewer)) {
            throw new DocumentNotFoundException(documentId);
        }

        return document;
    }

    @Transactional
    public Document update(
            UUID documentId,
            UpdateDocumentCommand command,
            User editor
    ) {
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(
                        () -> new DocumentNotFoundException(documentId)
                );

        if (!visibilityPolicy.canView(document, editor)) {
            throw new DocumentNotFoundException(documentId);
        }

        if (!editPolicy.canEdit(document, editor)) {
            throw new DocumentEditForbiddenException();
        }

        document.updateMetadata(
                command.title(),
                command.titleProvided(),
                command.description(),
                command.descriptionProvided()
        );

        return document;
    }

    @Transactional(readOnly = true)
    public Page<Document> listVisible(
            User viewer,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.asc("id")
                )
        );

        if (visibilityPolicy.canViewAll(viewer)) {
            return documentRepository.findAll(pageable);
        }

        return documentRepository.findByCreatedByIdOrStatus(
                viewer.getId(),
                DocumentStatus.APPROVED,
                pageable
        );
    }
}