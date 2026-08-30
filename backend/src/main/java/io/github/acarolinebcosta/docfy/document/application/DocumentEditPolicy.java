package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentEditPolicy {

    public boolean canEdit(Document document, User user) {
        if (document.getStatus() != DocumentStatus.DRAFT) {
            return false;
        }

        if (user.getRole() == Role.ADMIN
                || user.getRole() == Role.MANAGER) {
            return true;
        }

        return user.getRole() == Role.COLLABORATOR
                && document.getCreatedBy()
                        .getId()
                        .equals(user.getId());
    }
}