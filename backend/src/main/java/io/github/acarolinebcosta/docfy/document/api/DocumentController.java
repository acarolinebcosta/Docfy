package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.application.CreateDocumentCommand;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;
    private final UserRepository userRepository;

    public DocumentController(
            DocumentApplicationService documentApplicationService,
            UserRepository userRepository
    ) {
        this.documentApplicationService = documentApplicationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User createdBy = userRepository
                .findById(userId)
                .orElseThrow(AuthenticationException::new);

        Document document = documentApplicationService.create(
                new CreateDocumentCommand(
                        request.title(),
                        request.description()
                ),
                createdBy
        );

        DocumentResponse response =
                DocumentResponse.from(document);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/v1/documents/" + document.getId()
                        )
                )
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(
            @PathVariable UUID id
    ) {
        Document document =
                documentApplicationService.getById(id);

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }
}
