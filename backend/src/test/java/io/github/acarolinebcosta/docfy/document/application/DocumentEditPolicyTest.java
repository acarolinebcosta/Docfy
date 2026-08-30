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

class DocumentEditPolicyTest {

    private static final UUID EDITOR_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final DocumentEditPolicy policy =
            new DocumentEditPolicy();

    @Test
    void shouldAllowAdminToEditOtherUsersDraft() {
        assertTrue(
                policy.canEdit(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                DocumentStatus.DRAFT
                        ),
                        user(EDITOR_ID, Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowManagerToEditOtherUsersDraft() {
        assertTrue(
                policy.canEdit(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                DocumentStatus.DRAFT
                        ),
                        user(EDITOR_ID, Role.MANAGER)
                )
        );
    }

    @Test
    void shouldAllowCollaboratorToEditOwnDraft() {
        assertTrue(
                policy.canEdit(
                        documentOwnedBy(
                                EDITOR_ID,
                                DocumentStatus.DRAFT
                        ),
                        user(EDITOR_ID, Role.COLLABORATOR)
                )
        );
    }

    @Test
    void shouldDenyCollaboratorEditingOtherUsersDraft() {
        assertFalse(
                policy.canEdit(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                DocumentStatus.DRAFT
                        ),
                        user(EDITOR_ID, Role.COLLABORATOR)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "IN_REVIEW",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldDenyAdminEditingNonDraftDocument(
            DocumentStatus status
    ) {
        assertFalse(
                policy.canEdit(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                status
                        ),
                        user(EDITOR_ID, Role.ADMIN)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "IN_REVIEW",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldDenyManagerEditingNonDraftDocument(
            DocumentStatus status
    ) {
        assertFalse(
                policy.canEdit(
                        documentOwnedBy(
                                OTHER_USER_ID,
                                status
                        ),
                        user(EDITOR_ID, Role.MANAGER)
                )
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "IN_REVIEW",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldDenyCollaboratorEditingOwnNonDraftDocument(
            DocumentStatus status
    ) {
        assertFalse(
                policy.canEdit(
                        documentOwnedBy(
                                EDITOR_ID,
                                status
                        ),
                        user(EDITOR_ID, Role.COLLABORATOR)
                )
        );
    }

    private User user(
            UUID id,
            Role role
    ) {
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