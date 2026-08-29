package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;

class DocumentApplicationServiceTest {

    @Test
    void shouldCreateDraftDocument() {
        DocumentRepository repository =
                mock(DocumentRepository.class);

        DocumentApplicationService service =
                new DocumentApplicationService(repository);

        User user = new User(
                "creator@docfy.local",
                "{bcrypt}encoded-password",
                Role.COLLABORATOR
        );

        when(repository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateDocumentCommand command =
                new CreateDocumentCommand(
                        "Quality Strategy",
                        "Document quality strategy"
                );

        Document result = service.create(command, user);

        assertEquals("Quality Strategy", result.getTitle());
        assertEquals(
                "Document quality strategy",
                result.getDescription()
        );
        assertEquals(DocumentStatus.DRAFT, result.getStatus());
        assertEquals(user, result.getCreatedBy());

        verify(repository).save(result);
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateDocumentCommand(
                        "   ",
                        "Description"
                )
        );
    }

    @Test
    void shouldRejectTitleLongerThan255Characters() {
        String title = "A".repeat(256);

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateDocumentCommand(
                        title,
                        "Description"
                )
        );
    }
    @Test
void shouldReturnDocumentById() {
    DocumentRepository repository =
            mock(DocumentRepository.class);

    DocumentApplicationService service =
            new DocumentApplicationService(repository);

    User user = new User(
            "reader@docfy.local",
            "{bcrypt}encoded-password",
            Role.COLLABORATOR
    );

    Document document = new Document(
            "Quality Strategy",
            "Document quality strategy",
            user
    );

    UUID documentId =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    when(repository.findById(documentId))
            .thenReturn(Optional.of(document));

    Document result = service.getById(documentId);

    assertEquals(document, result);

    verify(repository).findById(documentId);
}

@Test
void shouldThrowWhenDocumentDoesNotExist() {
    DocumentRepository repository =
            mock(DocumentRepository.class);

    DocumentApplicationService service =
            new DocumentApplicationService(repository);

    UUID documentId =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    when(repository.findById(documentId))
            .thenReturn(Optional.empty());

    assertThrows(
            DocumentNotFoundException.class,
            () -> service.getById(documentId)
    );

    verify(repository).findById(documentId);
}
}
