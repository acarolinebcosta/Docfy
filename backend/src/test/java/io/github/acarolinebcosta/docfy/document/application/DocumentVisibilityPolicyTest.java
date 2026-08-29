package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentVisibilityPolicyTest {

    private static final UUID VIEWER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final DocumentVisibilityPolicy policy =
            new DocumentVisibilityPolicy();

    @ParameterizedTest
    @EnumSource(DocumentStatus.class)
    void shouldAllowAdminToViewOtherUsersDocumentsInEveryStatus(
            DocumentStatus status
    ) {
        assertTrue(
                policy.canView(
                        documentOwnedBy(OTHER_USER_ID, status),
                        user(VIEWER_ID, Role.ADMIN)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(DocumentStatus.class)
    void shouldAllowManagerToViewOtherUsersDocumentsInEveryStatus(
            DocumentStatus status
    ) {
        assertTrue(
                policy.canView(
                        documentOwnedBy(OTHER_USER_ID, status),
                        user(VIEWER_ID, Role.MANAGER)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(DocumentStatus.class)
    void shouldAllowCollaboratorToViewOwnDocumentsInEveryStatus(
            DocumentStatus status
    ) {
        assertTrue(
                policy.canView(
                        documentOwnedBy(VIEWER_ID, status),
                        user(VIEWER_ID, Role.COLLABORATOR)
                )
        );
    }

    @Test
    void shouldAllowCollaboratorToViewOtherUsersApprovedDocument() {
        assertTrue(
                policy.canView(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                DocumentStatus.APPROVED
                        ),
                        user(VIEWER_ID, Role.COLLABORATOR)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {"DRAFT", "IN_REVIEW", "ARCHIVED"}
    )
    void shouldDenyCollaboratorOtherUsersNonApprovedDocument(
            DocumentStatus status
    ) {
        assertFalse(
                policy.canView(
                        documentOwnedBy(OTHER_USER_ID, status),
                        user(VIEWER_ID, Role.COLLABORATOR)
                )
        );
    }

    private User user(UUID id, Role role) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);

        return user;
    }

    private Document documentOwnedBy(
            UUID ownerId,
            DocumentStatus status
    ) {
        User owner = mock(User.class);
        Document document = mock(Document.class);

        when(owner.getId()).thenReturn(ownerId);
        when(document.getCreatedBy()).thenReturn(owner);
        when(document.getStatus()).thenReturn(status);

        return document;
    }
}
