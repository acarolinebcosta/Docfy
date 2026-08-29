package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentApplicationService {

    private final DocumentRepository documentRepository;

    public DocumentApplicationService(
            DocumentRepository documentRepository
    ) {
        this.documentRepository = documentRepository;
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

    public Document getById(UUID documentId) {
        return documentRepository
                .findById(documentId)
                .orElseThrow(
                        () -> new DocumentNotFoundException(documentId)
                );
    }
}
