package io.github.acarolinebcosta.docfy.audit.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import org.springframework.stereotype.Component;

@Component
public class DocumentAuditPolicy {

    public boolean canViewAudit(User actor) {
        return actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.MANAGER;
    }
}