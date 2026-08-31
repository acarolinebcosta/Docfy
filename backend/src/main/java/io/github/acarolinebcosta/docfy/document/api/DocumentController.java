package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.application.CreateDocumentCommand;
import io.github.acarolinebcosta.docfy.document.application.DocumentApplicationService;
import io.github.acarolinebcosta.docfy.document.application.DocumentListCriteria;
import io.github.acarolinebcosta.docfy.document.application.UpdateDocumentCommand;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.shared.error.exception.BadRequestException;
import io.github.acarolinebcosta.docfy.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DocumentController {

    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MIN_PAGE_NUMBER = 0;
    private static final int MAX_PAGE_SIZE = 100;

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
        User createdBy = authenticatedUser(jwt);

        Document document = documentApplicationService.create(
                new CreateDocumentCommand(
                        request.title(),
                        request.description(),
                        request.categoryId()
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

    @PatchMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        validateUpdateRequest(request);

        Document document = documentApplicationService.update(
                id,
                new UpdateDocumentCommand(
                        request.getTitle(),
                        request.isTitleProvided(),
                        request.getDescription(),
                        request.isDescriptionProvided(),
                        request.getCategoryId(),
                        request.isCategoryProvided()
                ),
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Document document = documentApplicationService.getById(
                id,
                authenticatedUser(jwt)
        );

        return ResponseEntity.ok(
                DocumentResponse.from(document)
        );
    }

    @GetMapping
    public ResponseEntity<DocumentPageResponse> list(
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        validatePagination(page, size);

        Page<Document> documents = documentApplicationService.listVisible(
                authenticatedUser(jwt),
                page,
                size,
                new DocumentListCriteria(
                        search,
                        parseCategoryId(categoryId),
                        parseStatus(status)
                )
        );

        return ResponseEntity.ok(
                DocumentPageResponse.from(documents)
        );
    }

    private User authenticatedUser(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return userRepository
                .findById(userId)
                .orElseThrow(AuthenticationException::new);
    }

    private void validateUpdateRequest(
            UpdateDocumentRequest request
    ) {
        if (!request.hasAnyFieldProvided()) {
            throw new BadRequestException(
                    "At least one field must be provided"
            );
        }

        if (request.isTitleProvided()) {
            if (request.getTitle() == null) {
                throw new BadRequestException(
                        "Title must not be null"
                );
            }

            if (request.getTitle().isBlank()) {
                throw new BadRequestException(
                        "Title must not be blank"
                );
            }

            if (request.getTitle().length() > 255) {
                throw new BadRequestException(
                        "Title must not exceed 255 characters"
                );
            }
        }

        if (request.isCategoryProvided()
                && request.getCategoryId() == null) {
            throw new BadRequestException(
                    "Category must not be null"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < MIN_PAGE_NUMBER) {
            throw new BadRequestException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    private UUID parseCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(categoryId.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Category filter must be a valid UUID"
            );
        }
    }

    private DocumentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return DocumentStatus.valueOf(
                    status.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Invalid document status filter"
            );
        }
    }
}
