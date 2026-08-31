package io.github.acarolinebcosta.docfy.file.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.file.application.DocumentFileService;
import io.github.acarolinebcosta.docfy.file.application.DownloadedDocumentFile;
import io.github.acarolinebcosta.docfy.file.application.UploadDocumentFileCommand;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFile;
import io.github.acarolinebcosta.docfy.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/files")
@Tag(name = "Document files")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DocumentFileController {

    private final DocumentFileService documentFileService;
    private final UserRepository userRepository;

    public DocumentFileController(
            DocumentFileService documentFileService,
            UserRepository userRepository
    ) {
        this.documentFileService = documentFileService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentFileResponse> upload(
            @PathVariable UUID documentId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        DocumentFile stored = documentFileService.upload(
                documentId,
                new UploadDocumentFileCommand(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()
                ),
                authenticatedUser(jwt)
        );

        return ResponseEntity
                .created(
                        java.net.URI.create(
                                "/api/v1/documents/"
                                        + documentId
                                        + "/files/"
                                        + stored.getId()
                        )
                )
                .body(DocumentFileResponse.from(stored));
    }

    @GetMapping
    public ResponseEntity<List<DocumentFileResponse>> list(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<DocumentFileResponse> files = documentFileService
                .list(documentId, authenticatedUser(jwt))
                .stream()
                .map(DocumentFileResponse::from)
                .toList();

        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID documentId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        DownloadedDocumentFile file = documentFileService.download(
                documentId,
                fileId,
                authenticatedUser(jwt)
        );
        MediaType contentType = MediaType.parseMediaType(
                file.metadata().getContentType()
        );
        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(
                        file.metadata().getOriginalFilename(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(file.content().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(file.content());
    }

    private User authenticatedUser(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return userRepository
                .findById(userId)
                .orElseThrow(AuthenticationException::new);
    }
}
