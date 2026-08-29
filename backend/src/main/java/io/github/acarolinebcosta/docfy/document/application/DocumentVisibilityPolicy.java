package io.github.acarolinebcosta.docfy.document.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentVisibilityPolicy {

    public boolean canView(Document document, User viewer) {
        return canViewAll(viewer)
                || document.getCreatedBy().getId().equals(viewer.getId())
                || document.getStatus() == DocumentStatus.APPROVED;
    }

    public boolean canViewAll(User viewer) {
        return viewer.getRole() == Role.ADMIN
                || viewer.getRole() == Role.MANAGER;
    }
}
