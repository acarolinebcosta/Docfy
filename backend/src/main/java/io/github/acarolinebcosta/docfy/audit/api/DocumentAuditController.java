package io.github.acarolinebcosta.docfy.audit.api;

import io.github.acarolinebcosta.docfy.audit.application.DocumentAuditQueryService;
import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentAuditController {

    private final DocumentAuditQueryService auditQueryService;
    private final UserRepository userRepository;

    public DocumentAuditController(
            DocumentAuditQueryService auditQueryService,
            UserRepository userRepository
    ) {
        this.auditQueryService = auditQueryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/audit")
    public List<DocumentAuditEventResponse> history(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return auditQueryService
                .history(
                        id,
                        authenticatedUser(jwt)
                )
                .stream()
                .map(DocumentAuditEventResponse::from)
                .toList();
    }

    private User authenticatedUser(Jwt jwt) {
        UUID userId =
                UUID.fromString(jwt.getSubject());

        return userRepository
                .findById(userId)
                .orElseThrow(AuthenticationException::new);
    }
}