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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentUpdateIntegrationTest
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
    void shouldAllowCollaboratorToUpdateOwnDraft()
            throws Exception {

        User collaborator = createUser(
                "update-own",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Original title",
                "Original description",
                collaborator,
                DocumentStatus.DRAFT
        );

        patchDocument(
                document.getId(),
                tokenFor(collaborator),
                """
                {
                  "title": "Updated title",
                  "description": "Updated description"
                }
                """
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(document.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Updated title")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Updated description")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$.createdBy")
                                .value(
                                        collaborator
                                                .getId()
                                                .toString()
                                )
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Updated title",
                persisted.getTitle()
        );

        assertEquals(
                "Updated description",
                persisted.getDescription()
        );

        assertEquals(
                DocumentStatus.DRAFT,
                persisted.getStatus()
        );

        assertEquals(
                collaborator.getId(),
                persisted.getCreatedBy().getId()
        );
    }

    @Test
    void shouldAllowAdminToUpdateAnotherUsersDraft()
            throws Exception {

        User admin = createUser(
                "update-admin",
                Role.ADMIN
        );

        User owner = createUser(
                "update-admin-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Admin editable draft",
                "Original description",
                owner,
                DocumentStatus.DRAFT
        );

        patchDocument(
                document.getId(),
                tokenFor(admin),
                """
                {
                  "title": "Updated by admin"
                }
                """
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value("Updated by admin")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Original description")
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Updated by admin",
                persisted.getTitle()
        );

        assertEquals(
                "Original description",
                persisted.getDescription()
        );

        assertEquals(
                owner.getId(),
                persisted.getCreatedBy().getId()
        );
    }

    @Test
    void shouldAllowManagerToUpdateAnotherUsersDraft()
            throws Exception {

        User manager = createUser(
                "update-manager",
                Role.MANAGER
        );

        User owner = createUser(
                "update-manager-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Manager editable draft",
                "Original description",
                owner,
                DocumentStatus.DRAFT
        );

        patchDocument(
                document.getId(),
                tokenFor(manager),
                """
                {
                  "title": "Updated by manager"
                }
                """
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value("Updated by manager")
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Updated by manager",
                persisted.getTitle()
        );

        assertEquals(
                owner.getId(),
                persisted.getCreatedBy().getId()
        );
    }

    @Test
    void shouldClearDescriptionWhenNullIsExplicitlyProvided()
            throws Exception {

        User collaborator = createUser(
                "update-clear-description",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Quality Strategy",
                "Description to remove",
                collaborator,
                DocumentStatus.DRAFT
        );

        patchDocument(
                document.getId(),
                tokenFor(collaborator),
                """
                {
                  "description": null
                }
                """
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value("Quality Strategy")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value(nullValue())
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Quality Strategy",
                persisted.getTitle()
        );

        assertNull(
                persisted.getDescription()
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {
                    "IN_REVIEW",
                    "APPROVED",
                    "ARCHIVED"
            }
    )
    void shouldRejectCollaboratorEditingOwnNonDraftDocument(
            DocumentStatus documentStatus
    ) throws Exception {

        User collaborator = createUser(
                "update-locked-" + documentStatus.name(),
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Locked document",
                "Protected description",
                collaborator,
                documentStatus
        );

        patchDocument(
                document.getId(),
                tokenFor(collaborator),
                """
                {
                  "title": "Attempted update"
                }
                """
        )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("FORBIDDEN")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Forbidden")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/documents/"
                                                + document.getId()
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .exists()
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Locked document",
                persisted.getTitle()
        );

        assertEquals(
                "Protected description",
                persisted.getDescription()
        );
    }

    @Test
    void shouldConcealOtherUsersDraftFromCollaborator()
            throws Exception {

        User collaborator = createUser(
                "update-hidden-viewer",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "update-hidden-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Private draft",
                "Private description",
                owner,
                DocumentStatus.DRAFT
        );

        patchDocument(
                document.getId(),
                tokenFor(collaborator),
                """
                {
                  "title": "Attempted update"
                }
                """
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
                        jsonPath("$.correlationId")
                                .exists()
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Private draft",
                persisted.getTitle()
        );
    }

    @Test
    void shouldReturnForbiddenWhenCollaboratorCanViewButCannotEdit()
            throws Exception {

        User collaborator = createUser(
                "update-approved-viewer",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "update-approved-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Approved public document",
                "Approved description",
                owner,
                DocumentStatus.APPROVED
        );

        patchDocument(
                document.getId(),
                tokenFor(collaborator),
                """
                {
                  "title": "Attempted update"
                }
                """
        )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("FORBIDDEN")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Forbidden")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .exists()
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                "Approved public document",
                persisted.getTitle()
        );
    }

    @Test
    void shouldRejectEmptyPatch()
            throws Exception {

        User collaborator = createUser(
                "update-empty",
                Role.COLLABORATOR
        );

        UUID documentId = UUID.randomUUID();

        patchDocument(
                documentId,
                tokenFor(collaborator),
                """
                {}
                """
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "At least one field must be provided"
                                )
                );
    }

    @Test
    void shouldRejectNullTitle()
            throws Exception {

        User collaborator = createUser(
                "update-null-title",
                Role.COLLABORATOR
        );

        patchDocument(
                UUID.randomUUID(),
                tokenFor(collaborator),
                """
                {
                  "title": null
                }
                """
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Title must not be null")
                );
    }

    @Test
    void shouldRejectBlankTitle()
            throws Exception {

        User collaborator = createUser(
                "update-blank-title",
                Role.COLLABORATOR
        );

        patchDocument(
                UUID.randomUUID(),
                tokenFor(collaborator),
                """
                {
                  "title": "   "
                }
                """
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Title must not be blank")
                );
    }

    @Test
    void shouldRejectTitleLongerThan255Characters()
            throws Exception {

        User collaborator = createUser(
                "update-long-title",
                Role.COLLABORATOR
        );

        String longTitle =
                "A".repeat(256);

        String body = """
                {
                  "title": "%s"
                }
                """.formatted(longTitle);

        patchDocument(
                UUID.randomUUID(),
                tokenFor(collaborator),
                body
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Title must not exceed 255 characters"
                                )
                );
    }

    @Test
    void shouldRejectUnauthenticatedUpdate()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/documents/{id}",
                                UUID.randomUUID()
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "title": "Unauthorized update"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist()
            throws Exception {

        User collaborator = createUser(
                "update-not-found",
                Role.COLLABORATOR
        );

        UUID documentId = UUID.fromString(
                "99999999-9999-9999-9999-999999999999"
        );

        patchDocument(
                documentId,
                tokenFor(collaborator),
                """
                {
                  "title": "Updated title"
                }
                """
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
                        jsonPath("$.correlationId")
                                .exists()
                );
    }

    private ResultActions patchDocument(
            UUID documentId,
            String token,
            String body
    ) throws Exception {

        return mockMvc.perform(
                patch(
                        "/api/v1/documents/{id}",
                        documentId
                )
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(body)
        );
    }

    private User createUser(
            String prefix,
            Role role
    ) {
        return userRepository.save(
                new User(
                        prefix
                                + "-"
                                + UUID.randomUUID()
                                + "@docfy.local",
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                )
        );
    }

    private Document createDocument(
            String title,
            String description,
            User creator,
            DocumentStatus status
    ) {
        User managedCreator =
                userRepository
                        .findById(creator.getId())
                        .orElseThrow();

        Document document =
                documentRepository.saveAndFlush(
                        new Document(
                                title,
                                description,
                                managedCreator
                        )
                );

        if (status != DocumentStatus.DRAFT) {
            jdbcTemplate.update(
                    """
                    UPDATE documents
                    SET status = ?
                    WHERE id = ?
                    """,
                    status.name(),
                    document.getId()
            );
        }

        entityManager.clear();

        return documentRepository
                .findById(document.getId())
                .orElseThrow();
    }

    private Document reloadDocument(
            UUID documentId
    ) {
        entityManager.flush();
        entityManager.clear();

        return documentRepository
                .findById(documentId)
                .orElseThrow();
    }

    private String tokenFor(
            User user
    ) {
        Instant now =
                Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("docfy")
                        .issuedAt(now)
                        .expiresAt(
                                now.plusSeconds(3600)
                        )
                        .subject(
                                user.getId().toString()
                        )
                        .claim(
                                "email",
                                user.getEmail()
                        )
                        .claim(
                                "role",
                                user.getRole().name()
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                claims
                        )
                )
                .getTokenValue();
    }
}