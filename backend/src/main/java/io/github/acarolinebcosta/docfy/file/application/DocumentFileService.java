package io.github.acarolinebcosta.docfy.file.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFile;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFileRepository;
import io.github.acarolinebcosta.docfy.file.storage.FileStorage;
import io.github.acarolinebcosta.docfy.file.storage.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentFileService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocumentFileService.class);

    private final DocumentApplicationService documentApplicationService;
    private final DocumentFileRepository documentFileRepository;
    private final DocumentFileValidator validator;
    private final FileStorage fileStorage;

    public DocumentFileService(
            DocumentApplicationService documentApplicationService,
            DocumentFileRepository documentFileRepository,
            DocumentFileValidator validator,
            FileStorage fileStorage
    ) {
        this.documentApplicationService = documentApplicationService;
        this.documentFileRepository = documentFileRepository;
        this.validator = validator;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public DocumentFile upload(
            UUID documentId,
            UploadDocumentFileCommand command,
            User uploader
    ) {
        Document document = documentApplicationService.getEditableById(
                documentId,
                uploader
        );
        DocumentFileValidator.ValidatedDocumentFile validated =
                validator.validate(command);
        String storageKey = UUID.randomUUID() + "." + validated.extension();

        fileStorage.store(storageKey, validated.content());
        registerRollbackCompensation(storageKey);

        return documentFileRepository.saveAndFlush(
                new DocumentFile(
                        document,
                        validated.originalFilename(),
                        storageKey,
                        validated.contentType(),
                        validated.content().length,
                        uploader
                )
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentFile> list(UUID documentId, User viewer) {
        documentApplicationService.getById(documentId, viewer);

        return documentFileRepository
                .findByDocumentIdOrderByUploadedAtAscIdAsc(documentId);
    }

    @Transactional(readOnly = true)
    public DownloadedDocumentFile download(
            UUID documentId,
            UUID fileId,
            User viewer
    ) {
        documentApplicationService.getById(documentId, viewer);

        DocumentFile metadata = documentFileRepository
                .findByIdAndDocumentId(fileId, documentId)
                .orElseThrow(DocumentFileNotFoundException::new);

        return new DownloadedDocumentFile(
                metadata,
                fileStorage.load(metadata.getStorageKey())
        );
    }

    private void registerRollbackCompensation(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            removeStoredFile(storageKey);
                        }
                    }
                }
        );
    }

    private void removeStoredFile(String storageKey) {
        try {
            fileStorage.delete(storageKey);
        } catch (FileStorageException exception) {
            LOGGER.error(
                    "Could not compensate document file storage after database rollback. storageKey={}",
                    storageKey,
                    exception
            );
        }
    }
}

