package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.application.DocumentWorkflowService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentWorkflowController {

    private final DocumentWorkflowService workflowService;
    private final UserRepository userRepository;

    public DocumentWorkflowController(
            DocumentWorkflowService workflowService,
            UserRepository userRepository
    ) {
        this.workflowService = workflowService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<DocumentResponse> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Document document = workflowService.submit(
                id,
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DocumentResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Document document = workflowService.approve(
                id,
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DocumentResponse> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Document document = workflowService.reject(
                id,
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<DocumentResponse> archive(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Document document = workflowService.archive(
                id,
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    private User authenticatedUser(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return userRepository
                .findById(userId)
                .orElseThrow(AuthenticationException::new);
    }
}