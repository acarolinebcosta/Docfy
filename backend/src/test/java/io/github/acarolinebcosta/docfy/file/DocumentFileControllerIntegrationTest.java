package io.github.acarolinebcosta.docfy.file;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.file.domain.DocumentFileRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "docfy.files.storage-location=${java.io.tmpdir}/docfy-file-integration",
        "docfy.files.max-size=32B"
})
class DocumentFileControllerIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentFileRepository documentFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void shouldUploadListAndDownloadARealFile() throws Exception {
        User owner = createUser("file-owner", Role.COLLABORATOR);
        Document document = createDocument("File management", owner);
        MockMultipartFile upload = textFile("quality-notes.txt", "Quality evidence");

        String location = mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(upload)
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                matchesPattern("/api/v1/documents/.+/files/.+")
                        )
                )
                .andExpect(jsonPath("$.documentId").value(document.getId().toString()))
                .andExpect(jsonPath("$.originalFilename").value("quality-notes.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.size").value(16))
                .andExpect(jsonPath("$.uploadedBy").value(owner.getId().toString()))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String fileId = fileIdFromLocation(location).toString();

        mockMvc.perform(
                        get("/api/v1/documents/{id}/files", document.getId())
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(fileId))
                .andExpect(jsonPath("$[0].originalFilename").value("quality-notes.txt"));

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{documentId}/files/{fileId}",
                                document.getId(),
                                fileId
                        ).header("Authorization", bearer(owner))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                containsString("quality-notes.txt")
                        )
                )
                .andExpect(content().bytes("Quality evidence".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"MANAGER", "ADMIN"}
    )
    void shouldAllowPrivilegedRoleToUploadToAnotherUsersDraft(
            Role role
    ) throws Exception {
        User owner = createUser("privileged-file-owner", Role.COLLABORATOR);
        User privilegedUser = createUser("file-privileged", role);
        Document document = createDocument("Managed draft", owner);

        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(textFile("manager.txt", "Manager evidence"))
                                .header("Authorization", bearer(privilegedUser))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnSafe404WhenUploadDocumentDoesNotExist()
            throws Exception {
        User user = createUser("missing-file-document", Role.ADMIN);

        mockMvc.perform(
                        multipart(
                                "/api/v1/documents/{id}/files",
                                UUID.randomUUID()
                        )
                                .file(textFile("missing.txt", "Missing"))
                                .header("Authorization", bearer(user))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));
    }

    @Test
    void shouldConcealAnotherUsersPrivateDraftFromCollaborator() throws Exception {
        User owner = createUser("private-file-owner", Role.COLLABORATOR);
        User viewer = createUser("private-file-viewer", Role.COLLABORATOR);
        Document document = createDocument("Private draft file", owner);
        long filesBefore = documentFileRepository.count();

        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(textFile("private.txt", "Private"))
                                .header("Authorization", bearer(viewer))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));

        org.junit.jupiter.api.Assertions.assertEquals(
                filesBefore,
                documentFileRepository.count()
        );
    }

    @Test
    void shouldForbidUploadWhenVisibleDocumentIsNotDraft() throws Exception {
        User owner = createUser("approved-file-owner", Role.COLLABORATOR);
        User manager = createUser("approved-file-manager", Role.MANAGER);
        Document document = createDocument("Approved file", owner);
        document.submitForReview();
        document.approve();
        documentRepository.flush();

        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(textFile("approved.txt", "Approved"))
                                .header("Authorization", bearer(manager))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void shouldValidateEmptySizeExtensionAndContentSignature() throws Exception {
        User owner = createUser("invalid-file-owner", Role.COLLABORATOR);
        Document document = createDocument("Invalid files", owner);

        expectBadUpload(document, owner, textFile("empty.txt", ""), "File must not be empty");
        expectBadUpload(
                document,
                owner,
                new MockMultipartFile("file", "malware.exe", "application/octet-stream", "MZ".getBytes()),
                "File type is not allowed"
        );
        expectBadUpload(
                document,
                owner,
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not-a-pdf".getBytes()),
                "File content does not match its declared type"
        );
        expectBadUpload(
                document,
                owner,
                textFile("large.txt", "x".repeat(33)),
                "File exceeds the maximum allowed size"
        );
    }

    @Test
    void shouldRequireAuthenticationForFileEndpoints() throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/documents/{id}/files", documentId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", documentId)
                                .file(textFile("anonymous.txt", "Anonymous"))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListEmptyAndMultipleFilesForVisibleDocument()
            throws Exception {
        User owner = createUser("list-files-owner", Role.COLLABORATOR);
        Document document = createDocument("List files", owner);

        mockMvc.perform(
                        get("/api/v1/documents/{id}/files", document.getId())
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        uploadFile(document, owner, "first.txt", "First");
        uploadFile(document, owner, "second.txt", "Second");

        mockMvc.perform(
                        get("/api/v1/documents/{id}/files", document.getId())
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[*].originalFilename")
                                .value(hasItems("first.txt", "second.txt"))
                );
    }

    @Test
    void shouldConcealFileListingForInvisibleDocument()
            throws Exception {
        User owner = createUser("hidden-list-owner", Role.COLLABORATOR);
        User viewer = createUser("hidden-list-viewer", Role.COLLABORATOR);
        Document document = createDocument("Hidden attachments", owner);

        mockMvc.perform(
                        get("/api/v1/documents/{id}/files", document.getId())
                                .header("Authorization", bearer(viewer))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));
    }

    @Test
    void shouldReturn404ForMissingFileWithinVisibleDocument()
            throws Exception {
        User owner = createUser("missing-download-owner", Role.COLLABORATOR);
        Document document = createDocument("Missing download", owner);

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{documentId}/files/{fileId}",
                                document.getId(),
                                UUID.randomUUID()
                        ).header("Authorization", bearer(owner))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("File not found"));
    }

    @Test
    void shouldConcealDownloadWhenDocumentIsInvisible()
            throws Exception {
        User owner = createUser("hidden-download-owner", Role.COLLABORATOR);
        User viewer = createUser("hidden-download-viewer", Role.COLLABORATOR);
        Document document = createDocument("Hidden download", owner);
        UUID fileId = uploadFile(document, owner, "private.txt", "Private");

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{documentId}/files/{fileId}",
                                document.getId(),
                                fileId
                        ).header("Authorization", bearer(viewer))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));
    }

    @Test
    void shouldAllowCollaboratorToDownloadAnotherUsersApprovedFile()
            throws Exception {
        User owner = createUser("approved-download-owner", Role.COLLABORATOR);
        User viewer = createUser("approved-download-viewer", Role.COLLABORATOR);
        Document document = createDocument("Approved download", owner);
        UUID fileId = uploadFile(document, owner, "approved.txt", "Approved");
        document.submitForReview();
        document.approve();
        documentRepository.flush();

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{documentId}/files/{fileId}",
                                document.getId(),
                                fileId
                        ).header("Authorization", bearer(viewer))
                )
                .andExpect(status().isOk())
                .andExpect(content().bytes("Approved".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldKeepOnlySafeBasenameAsMetadata() throws Exception {
        User owner = createUser("filename-owner", Role.COLLABORATOR);
        Document document = createDocument("Filename safety", owner);

        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(textFile("../../quality.txt", "Safe"))
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("quality.txt"));
    }

    private void expectBadUpload(
            Document document,
            User owner,
            MockMultipartFile file,
            String message
    ) throws Exception {
        mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(file)
                                .header("Authorization", bearer(owner))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    private MockMultipartFile textFile(String name, String value) {
        return new MockMultipartFile(
                "file",
                name,
                MediaType.TEXT_PLAIN_VALUE,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private UUID uploadFile(
            Document document,
            User uploader,
            String filename,
            String content
    ) throws Exception {
        String location = mockMvc.perform(
                        multipart("/api/v1/documents/{id}/files", document.getId())
                                .file(textFile(filename, content))
                                .header("Authorization", bearer(uploader))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        return fileIdFromLocation(location);
    }

    private UUID fileIdFromLocation(String location) {
        if (location == null) {
            throw new IllegalStateException("Upload Location header is missing");
        }

        return UUID.fromString(
                location.substring(location.lastIndexOf('/') + 1)
        );
    }

    private User createUser(String prefix, Role role) {
        return userRepository.saveAndFlush(
                new User(
                        prefix + "-" + UUID.randomUUID() + "@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        role
                )
        );
    }

    private Document createDocument(String title, User owner) {
        return documentRepository.saveAndFlush(
                new Document(title, "Attachment test", owner)
        );
    }

    private String bearer(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("docfy")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return "Bearer " + jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}
