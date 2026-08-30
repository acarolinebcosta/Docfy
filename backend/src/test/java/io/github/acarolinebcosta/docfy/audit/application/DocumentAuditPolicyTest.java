package io.github.acarolinebcosta.docfy.audit.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentAuditPolicyTest {

    private final DocumentAuditPolicy policy =
            new DocumentAuditPolicy();

    @Test
    void shouldAllowAdminToViewAudit() {
        assertTrue(
                policy.canViewAudit(
                        user(Role.ADMIN)
                )
        );
    }

    @Test
    void shouldAllowManagerToViewAudit() {
        assertTrue(
                policy.canViewAudit(
                        user(Role.MANAGER)
                )
        );
    }

    @Test
    void shouldNotAllowCollaboratorToViewAudit() {
        assertFalse(
                policy.canViewAudit(
                        user(Role.COLLABORATOR)
                )
        );
    }

    private User user(Role role) {
        return new User(
                role.name().toLowerCase()
                        + "@docfy.test",
                "password-hash",
                role
        );
    }
}