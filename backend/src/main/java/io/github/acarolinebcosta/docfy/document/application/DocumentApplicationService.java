package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.category.application.CategoryNotFoundException;
import io.github.acarolinebcosta.docfy.category.domain.Category;
import io.github.acarolinebcosta.docfy.category.domain.CategoryRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentApplicationService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentVisibilityPolicy visibilityPolicy;
    private final DocumentEditPolicy editPolicy;

    public DocumentApplicationService(
            DocumentRepository documentRepository,
            CategoryRepository categoryRepository,
            DocumentVisibilityPolicy visibilityPolicy,
            DocumentEditPolicy editPolicy
    ) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.visibilityPolicy = visibilityPolicy;
        this.editPolicy = editPolicy;
    }

    @Transactional
    public Document create(
            CreateDocumentCommand command,
            User createdBy
    ) {
        Category category = findCategory(command.categoryId());
        Document document = new Document(
                command.title(),
                command.description(),
                createdBy,
                category
        );

        return documentRepository.saveAndFlush(document);
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
        Document document = getEditableById(documentId, editor);

        Category category = command.categoryProvided()
                ? findCategory(command.categoryId())
                : null;

        document.updateMetadata(
                command.title(),
                command.titleProvided(),
                command.description(),
                command.descriptionProvided(),
                category,
                command.categoryProvided()
        );

        return document;
    }

    @Transactional(readOnly = true)
    public Document getEditableById(
            UUID documentId,
            User editor
    ) {
        Document document = getById(documentId, editor);

        if (!editPolicy.canEdit(document, editor)) {
            throw new DocumentEditForbiddenException();
        }

        return document;
    }

    @Transactional(readOnly = true)
    public Page<Document> listVisible(
            User viewer,
            int page,
            int size,
            DocumentListCriteria criteria
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.asc("id")
                )
        );

        String searchPattern = criteria.search() == null
                ? null
                : "%" + criteria.search().toLowerCase(Locale.ROOT) + "%";

        return documentRepository.findVisibleByCriteria(
                visibilityPolicy.canViewAll(viewer),
                viewer.getId(),
                DocumentStatus.APPROVED,
                searchPattern,
                criteria.categoryId(),
                criteria.status(),
                pageable
        );
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(
                        () -> new CategoryNotFoundException(categoryId)
                );
    }
}
