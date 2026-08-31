package io.github.acarolinebcosta.docfy.file;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.file.application.DocumentFileService;
import io.github.acarolinebcosta.docfy.file.application.DocumentFileValidator;
import io.github.acarolinebcosta.docfy.file.application.UploadDocumentFileCommand;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFile;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFileRepository;
import io.github.acarolinebcosta.docfy.file.storage.FileStorage;
import io.github.acarolinebcosta.docfy.file.storage.FileStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentFileServiceTest {

    private final DocumentApplicationService documentApplicationService =
            mock(DocumentApplicationService.class);
    private final DocumentFileRepository repository =
            mock(DocumentFileRepository.class);
    private final DocumentFileValidator validator =
            mock(DocumentFileValidator.class);
    private final FileStorage storage = mock(FileStorage.class);
    private final DocumentFileService service = new DocumentFileService(
            documentApplicationService,
            repository,
            validator,
            storage
    );

    @BeforeEach
    void startTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRemoveStoredFileWhenMetadataPersistenceFails() {
        User uploader = user();
        Document document = new Document(
                "Compensation",
                "Rollback evidence",
                uploader
        );
        UUID documentId = UUID.randomUUID();
        UploadDocumentFileCommand command = command();

        when(documentApplicationService.getEditableById(documentId, uploader))
                .thenReturn(document);
        when(validator.validate(command)).thenReturn(validated());
        when(repository.saveAndFlush(any(DocumentFile.class)))
                .thenThrow(new DataIntegrityViolationException("database failure"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.upload(documentId, command, uploader)
        );

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(
                                TransactionSynchronization.STATUS_ROLLED_BACK
                        )
                );

        verify(storage).store(anyString(), any(byte[].class));
        verify(storage).delete(anyString());
    }

    @Test
    void shouldNotPersistMetadataWhenStorageWriteFails() {
        User uploader = user();
        Document document = new Document(
                "Storage failure",
                "No metadata",
                uploader
        );
        UUID documentId = UUID.randomUUID();
        UploadDocumentFileCommand command = command();

        when(documentApplicationService.getEditableById(documentId, uploader))
                .thenReturn(document);
        when(validator.validate(command)).thenReturn(validated());
        org.mockito.Mockito.doThrow(
                        new FileStorageException(
                                "storage unavailable",
                                new java.io.IOException("disk failure")
                        )
                )
                .when(storage)
                .store(anyString(), any(byte[].class));

        assertThrows(
                FileStorageException.class,
                () -> service.upload(documentId, command, uploader)
        );

        verify(repository, never()).saveAndFlush(any(DocumentFile.class));
    }

    private UploadDocumentFileCommand command() {
        return new UploadDocumentFileCommand(
                "evidence.txt",
                "text/plain",
                "evidence".getBytes(StandardCharsets.UTF_8)
        );
    }

    private DocumentFileValidator.ValidatedDocumentFile validated() {
        return new DocumentFileValidator.ValidatedDocumentFile(
                "evidence.txt",
                "text/plain",
                "txt",
                "evidence".getBytes(StandardCharsets.UTF_8)
        );
    }

    private User user() {
        return new User(
                "files-service@docfy.local",
                "{noop}password",
                Role.COLLABORATOR
        );
    }
}
