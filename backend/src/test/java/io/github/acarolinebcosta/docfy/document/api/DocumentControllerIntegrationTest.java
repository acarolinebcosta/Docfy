package io.github.acarolinebcosta.docfy.document.api;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTest
        implements PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldCreateDraftDocumentForAuthenticatedUser()
            throws Exception {

        User user = userRepository.save(
                new User(
                        "document-api@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        Role.COLLABORATOR
                )
        );

        String token = tokenFor(user);
        long documentCountBefore = documentRepository.count();

        MvcResult result = mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Quality Strategy",
                                          "description": "Document quality strategy",
                                          "categoryId": "11111111-0000-0000-0000-000000000007"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                org.hamcrest.Matchers.matchesPattern(
                                        "/api/v1/documents/.+"
                                )
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Quality Strategy")
                )
                .andExpect(
                        jsonPath("$.documentCode")
                                .value(
                                        org.hamcrest.Matchers.matchesPattern(
                                                "DOC-\\d{6,}"
                                        )
                                )
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Document quality strategy")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$.createdBy")
                                .value(user.getId().toString())
                )
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertEquals(
                documentCountBefore + 1,
                documentRepository.count()
        );

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);

        UUID documentId = UUID.fromString(
                location.substring(location.lastIndexOf('/') + 1)
        );

        Document persisted = documentRepository
                .findById(documentId)
                .orElseThrow();

        assertEquals("Quality Strategy", persisted.getTitle());
        assertEquals(user.getId(), persisted.getCreatedBy().getId());
    }

    @Test
    void shouldRejectBlankTitle()
            throws Exception {

        User user = userRepository.save(
                new User(
                        "document-blank@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        Role.COLLABORATOR
                )
        );

        long documentCountBefore = documentRepository.count();

        mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "   ",
                                          "description": "Invalid",
                                          "categoryId": "11111111-0000-0000-0000-000000000007"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                documentCountBefore,
                documentRepository.count()
        );
    }

    @Test
    void shouldRejectTitleLongerThan255Characters()
            throws Exception {

        User user = userRepository.save(
                new User(
                        "document-long-title@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        Role.COLLABORATOR
                )
        );

        String longTitle = "A".repeat(256);
        long documentCountBefore = documentRepository.count();

        String body = """
                {
                  "title": "%s",
                  "description": "Invalid",
                  "categoryId": "11111111-0000-0000-0000-000000000007"
                }
                """.formatted(longTitle);

        mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                documentCountBefore,
                documentRepository.count()
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequest()
            throws Exception {
        long documentCountBefore = documentRepository.count();

        mockMvc.perform(
                        post("/api/v1/documents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Quality Strategy",
                                          "description": "Document quality strategy",
                                          "categoryId": "11111111-0000-0000-0000-000000000007"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        assertEquals(
                documentCountBefore,
                documentRepository.count()
        );
    }

    @Test
    void shouldReturnDocumentByIdForAuthenticatedUser()
            throws Exception {

        User user = userRepository.save(
                new User(
                        "document-get@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        Role.COLLABORATOR
                )
        );

        Document document = documentRepository.saveAndFlush(
                new Document(
                        "Architecture",
                        "System architecture document",
                        user
                )
        );

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(document.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Architecture")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$.createdBy")
                                .value(user.getId().toString())
                );
    }

    @Test
    void shouldAllowAdminToReadAnotherUsersDraft()
            throws Exception {
        User admin = createUser("get-admin", Role.ADMIN);
        User owner = createUser("get-admin-owner", Role.COLLABORATOR);
        Document document = createDocument(
                "Admin visible draft",
                owner,
                DocumentStatus.DRAFT
        );

        expectVisibleDocument(document, admin);
    }

    @Test
    void shouldAllowManagerToReadAnotherUsersDraft()
            throws Exception {
        User manager = createUser("get-manager", Role.MANAGER);
        User owner = createUser(
                "get-manager-owner",
                Role.COLLABORATOR
        );
        Document document = createDocument(
                "Manager visible draft",
                owner,
                DocumentStatus.DRAFT
        );

        expectVisibleDocument(document, manager);
    }

    @Test
    void shouldAllowCollaboratorToReadAnotherUsersApprovedDocument()
            throws Exception {
        User collaborator = createUser(
                "get-approved-viewer",
                Role.COLLABORATOR
        );
        User owner = createUser(
                "get-approved-owner",
                Role.COLLABORATOR
        );
        Document document = createDocument(
                "Approved document",
                owner,
                DocumentStatus.APPROVED
        );

        expectVisibleDocument(document, collaborator);
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {"DRAFT", "IN_REVIEW", "ARCHIVED"}
    )
    void shouldConcealAnotherUsersNonApprovedDocumentFromCollaborator(
            DocumentStatus status
    ) throws Exception {
        User collaborator = createUser(
                "get-concealed-viewer-" + status.name(),
                Role.COLLABORATOR
        );
        User owner = createUser(
                "get-concealed-owner-" + status.name(),
                Role.COLLABORATOR
        );
        Document document = createDocument(
                "Concealed " + status.name(),
                owner,
                status
        );

        expectNotFound(document.getId(), tokenFor(collaborator));
    }

    @Test
    void shouldReturnNotFoundForUnknownDocument()
            throws Exception {

        User user = userRepository.save(
                new User(
                        "document-not-found@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        Role.COLLABORATOR
                )
        );

        String documentId =
                "33333333-3333-3333-3333-333333333333";

        expectNotFound(
                UUID.fromString(documentId),
                tokenFor(user)
        );
    }

    @Test
    void shouldRejectUnauthenticatedDocumentLookup()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}",
                                "44444444-4444-4444-4444-444444444444"
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    private String tokenFor(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("docfy")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(claims)
                )
                .getTokenValue();
    }

    private User createUser(String prefix, Role role) {
        return userRepository.save(
                new User(
                        prefix + "-" + UUID.randomUUID()
                                + "@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        role
                )
        );
    }

    private Document createDocument(
            String title,
            User creator,
            DocumentStatus status
    ) {
        User managedCreator = userRepository
                .findById(creator.getId())
                .orElseThrow();
        Document document = documentRepository.saveAndFlush(
                new Document(
                        title,
                        "Document visibility integration test",
                        managedCreator
                )
        );

        if (status != DocumentStatus.DRAFT) {
            jdbcTemplate.update(
                    "UPDATE documents SET status = ? WHERE id = ?",
                    status.name(),
                    document.getId()
            );
        }

        entityManager.clear();

        return documentRepository
                .findById(document.getId())
                .orElseThrow();
    }

    private void expectVisibleDocument(
            Document document,
            User viewer
    ) throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(viewer)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(document.getId().toString())
                );
    }

    private void expectNotFound(
            UUID documentId,
            String token
    ) throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}",
                                documentId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Document not found")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/documents/"
                                                + documentId
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId").exists()
                );
    }
}
