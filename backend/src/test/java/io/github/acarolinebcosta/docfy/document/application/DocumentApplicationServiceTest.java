package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentApplicationServiceTest {

    @Test
    void shouldCreateDraftDocument() {
        DocumentRepository repository =
                mock(DocumentRepository.class);

        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        mock(DocumentVisibilityPolicy.class)
                );

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
    void shouldReturnVisibleDocumentById() {
        DocumentRepository repository =
                mock(DocumentRepository.class);
        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);
        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        visibilityPolicy
                );
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
        UUID documentId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        when(repository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(visibilityPolicy.canView(document, user))
                .thenReturn(true);

        Document result = service.getById(documentId, user);

        assertEquals(document, result);
        verify(repository).findById(documentId);
        verify(visibilityPolicy).canView(document, user);
    }

    @Test
    void shouldConcealDocumentThatViewerCannotSee() {
        DocumentRepository repository =
                mock(DocumentRepository.class);
        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);
        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        visibilityPolicy
                );
        User viewer = new User(
                "viewer@docfy.local",
                "{bcrypt}encoded-password",
                Role.COLLABORATOR
        );
        User owner = new User(
                "owner@docfy.local",
                "{bcrypt}encoded-password",
                Role.COLLABORATOR
        );
        Document document = new Document(
                "Private draft",
                "Another user's document",
                owner
        );
        UUID documentId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );

        when(repository.findById(documentId))
                .thenReturn(Optional.of(document));
        when(visibilityPolicy.canView(document, viewer))
                .thenReturn(false);

        assertThrows(
                DocumentNotFoundException.class,
                () -> service.getById(documentId, viewer)
        );

        verify(repository).findById(documentId);
        verify(visibilityPolicy).canView(document, viewer);
    }

    @Test
    void shouldThrowWhenDocumentDoesNotExist() {
        DocumentRepository repository =
                mock(DocumentRepository.class);
        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);
        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        visibilityPolicy
                );
        User viewer = new User(
                "missing-reader@docfy.local",
                "{bcrypt}encoded-password",
                Role.COLLABORATOR
        );
        UUID documentId = UUID.fromString(
                "33333333-3333-3333-3333-333333333333"
        );

        when(repository.findById(documentId))
                .thenReturn(Optional.empty());

        assertThrows(
                DocumentNotFoundException.class,
                () -> service.getById(documentId, viewer)
        );

        verify(repository).findById(documentId);
    }

    @Test
    void shouldListEveryDocumentForUnrestrictedViewer() {
        DocumentRepository repository =
                mock(DocumentRepository.class);
        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);
        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        visibilityPolicy
                );
        User viewer = mockUser(Role.ADMIN);
        Page<Document> expected = new PageImpl<>(List.of());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(visibilityPolicy.canViewAll(viewer)).thenReturn(true);
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(expected);

        Page<Document> result = service.listVisible(viewer, 2, 10);

        assertEquals(expected, result);
        verify(repository).findAll(pageableCaptor.capture());
        assertPageable(pageableCaptor.getValue(), 2, 10);
    }

    @Test
    void shouldQueryOnlyOwnOrApprovedDocumentsForCollaborator() {
        DocumentRepository repository =
                mock(DocumentRepository.class);
        DocumentVisibilityPolicy visibilityPolicy =
                mock(DocumentVisibilityPolicy.class);
        DocumentApplicationService service =
                new DocumentApplicationService(
                        repository,
                        visibilityPolicy
                );
        User viewer = mockUser(Role.COLLABORATOR);
        Page<Document> expected = new PageImpl<>(List.of());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(visibilityPolicy.canViewAll(viewer)).thenReturn(false);
        when(
                repository.findByCreatedByIdOrStatus(
                        eq(viewer.getId()),
                        eq(DocumentStatus.APPROVED),
                        any(Pageable.class)
                )
        ).thenReturn(expected);

        Page<Document> result = service.listVisible(viewer, 0, 20);

        assertEquals(expected, result);
        verify(repository).findByCreatedByIdOrStatus(
                eq(viewer.getId()),
                eq(DocumentStatus.APPROVED),
                pageableCaptor.capture()
        );
        assertPageable(pageableCaptor.getValue(), 0, 20);
    }

    private void assertPageable(
            Pageable pageable,
            int expectedPage,
            int expectedSize
    ) {
        assertEquals(expectedPage, pageable.getPageNumber());
        assertEquals(expectedSize, pageable.getPageSize());

        Sort.Order updatedAt = pageable.getSort().getOrderFor("updatedAt");
        Sort.Order id = pageable.getSort().getOrderFor("id");

        assertTrue(updatedAt != null && updatedAt.isDescending());
        assertTrue(id != null && id.isAscending());
    }

    private User mockUser(Role role) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getRole()).thenReturn(role);

        return user;
    }
}
