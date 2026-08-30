package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentWorkflowPolicyTest {

    private final DocumentWorkflowPolicy policy =
            new DocumentWorkflowPolicy();

    @Test
    void shouldAllowAdminToSubmitAnyDocument() {
        User owner = user(Role.COLLABORATOR);
        User admin = user(Role.ADMIN);

        Document document = documentOwnedBy(owner);

        assertTrue(
                policy.canSubmit(document, admin)
        );
    }

    @Test
    void shouldAllowManagerToSubmitAnyDocument() {
        User owner = user(Role.COLLABORATOR);
        User manager = user(Role.MANAGER);

        Document document = documentOwnedBy(owner);

        assertTrue(
                policy.canSubmit(document, manager)
        );
    }

    @Test
    void shouldAllowCollaboratorToSubmitOwnDocument() {
        User collaborator = user(Role.COLLABORATOR);

        Document document =
                documentOwnedBy(collaborator);

        assertTrue(
                policy.canSubmit(
                        document,
                        collaborator
                )
        );
    }

    @Test
    void shouldDenyCollaboratorSubmittingAnotherUsersDocument() {
        User owner = user(Role.COLLABORATOR);
        User collaborator = user(Role.COLLABORATOR);

        Document document = documentOwnedBy(owner);

        assertFalse(
                policy.canSubmit(
                        document,
                        collaborator
                )
        );
    }

    @Test
    void shouldAllowAdminToApprove() {
        assertTrue(
                policy.canApprove(
                        user(Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowManagerToApprove() {
        assertTrue(
                policy.canApprove(
                        user(Role.MANAGER)
                )
        );
    }

    @Test
    void shouldDenyCollaboratorApproval() {
        assertFalse(
                policy.canApprove(
                        user(Role.COLLABORATOR)
                )
        );
    }

    @Test
    void shouldAllowAdminToReject() {
        assertTrue(
                policy.canReject(
                        user(Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowManagerToReject() {
        assertTrue(
                policy.canReject(
                        user(Role.MANAGER)
                )
        );
    }

    @Test
    void shouldDenyCollaboratorRejection() {
        assertFalse(
                policy.canReject(
                        user(Role.COLLABORATOR)
                )
        );
    }

    @Test
    void shouldAllowAdminToArchive() {
        assertTrue(
                policy.canArchive(
                        user(Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowManagerToArchive() {
        assertTrue(
                policy.canArchive(
                        user(Role.MANAGER)
                )
        );
    }

    @Test
    void shouldDenyCollaboratorArchiving() {
        assertFalse(
                policy.canArchive(
                        user(Role.COLLABORATOR)
                )
        );
    }

    private Document documentOwnedBy(User owner) {
        return new Document(
                "Workflow document",
                "Workflow authorization test",
                owner
        );
    }

    private User user(Role role) {
        User user = new User(
                UUID.randomUUID() + "@docfy.test",
                "password-hash",
                role
        );

        setId(user, UUID.randomUUID());

        return user;
    }

    private void setId(
            User user,
            UUID id
    ) {
        try {
            Field field = User.class
                    .getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to configure test user id",
                    exception
            );
        }
    }
}