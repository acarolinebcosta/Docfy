package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentWorkflowPolicy {

    public boolean canSubmit(
            Document document,
            User user
    ) {
        if (user.getRole() == Role.ADMIN
                || user.getRole() == Role.MANAGER) {
            return true;
        }

        return user.getRole() == Role.COLLABORATOR
                && document.getCreatedBy()
                        .getId()
                        .equals(user.getId());
    }

    public boolean canApprove(User user) {
        return user.getRole() == Role.ADMIN
                || user.getRole() == Role.MANAGER;
    }

    public boolean canReject(User user) {
        return user.getRole() == Role.ADMIN
                || user.getRole() == Role.MANAGER;
    }

    public boolean canArchive(User user) {
        return user.getRole() == Role.ADMIN
                || user.getRole() == Role.MANAGER;
    }
}