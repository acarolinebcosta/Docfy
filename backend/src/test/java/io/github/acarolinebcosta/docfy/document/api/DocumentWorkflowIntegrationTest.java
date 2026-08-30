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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentWorkflowIntegrationTest
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
    void shouldCompleteDocumentLifecycle()
            throws Exception {

        User manager = createUser(
                "workflow-manager",
                Role.MANAGER
        );

        User owner = createUser(
                "workflow-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Lifecycle document",
                "Full workflow",
                owner,
                DocumentStatus.DRAFT
        );

        String token = tokenFor(manager);

        workflowAction(
                document.getId(),
                "submit",
                token
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("IN_REVIEW")
                );

        assertEquals(
                DocumentStatus.IN_REVIEW,
                reloadDocument(document.getId())
                        .getStatus()
        );

        workflowAction(
                document.getId(),
                "approve",
                token
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("APPROVED")
                );

        assertEquals(
                DocumentStatus.APPROVED,
                reloadDocument(document.getId())
                        .getStatus()
        );

        workflowAction(
                document.getId(),
                "archive",
                token
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("ARCHIVED")
                );

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                DocumentStatus.ARCHIVED,
                persisted.getStatus()
        );

        assertEquals(
                owner.getId(),
                persisted.getCreatedBy().getId()
        );
    }

    @Test
    void shouldAllowCollaboratorToSubmitOwnDraft()
            throws Exception {

        User collaborator = createUser(
                "workflow-own-submit",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Own draft",
                "Collaborator submission",
                collaborator,
                DocumentStatus.DRAFT
        );

        workflowAction(
                document.getId(),
                "submit",
                tokenFor(collaborator)
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        document.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("IN_REVIEW")
                );

        assertEquals(
                DocumentStatus.IN_REVIEW,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldAllowManagerToRejectDocumentInReview()
            throws Exception {

        User manager = createUser(
                "workflow-reject-manager",
                Role.MANAGER
        );

        User owner = createUser(
                "workflow-reject-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Review document",
                "Document to reject",
                owner,
                DocumentStatus.IN_REVIEW
        );

        workflowAction(
                document.getId(),
                "reject",
                tokenFor(manager)
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        assertEquals(
                DocumentStatus.DRAFT,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldAllowAdminToApproveDocumentInReview()
            throws Exception {

        User admin = createUser(
                "workflow-approve-admin",
                Role.ADMIN
        );

        User owner = createUser(
                "workflow-approve-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Approval document",
                "Document to approve",
                owner,
                DocumentStatus.IN_REVIEW
        );

        workflowAction(
                document.getId(),
                "approve",
                tokenFor(admin)
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("APPROVED")
                );

        assertEquals(
                DocumentStatus.APPROVED,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenCollaboratorTriesToApprove()
            throws Exception {

        User collaborator = createUser(
                "workflow-forbidden-approve",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Own review",
                "Cannot approve own document",
                collaborator,
                DocumentStatus.IN_REVIEW
        );

        workflowAction(
                document.getId(),
                "approve",
                tokenFor(collaborator)
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

        assertEquals(
                DocumentStatus.IN_REVIEW,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldReturnForbiddenWhenCollaboratorTriesToArchiveVisibleApprovedDocument()
            throws Exception {

        User collaborator = createUser(
                "workflow-forbidden-archive",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "workflow-public-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Approved document",
                "Visible but protected",
                owner,
                DocumentStatus.APPROVED
        );

        workflowAction(
                document.getId(),
                "archive",
                tokenFor(collaborator)
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

        assertEquals(
                DocumentStatus.APPROVED,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldConcealOtherUsersDraftFromCollaboratorSubmitting()
            throws Exception {

        User collaborator = createUser(
                "workflow-hidden-viewer",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "workflow-hidden-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Private draft",
                "Hidden document",
                owner,
                DocumentStatus.DRAFT
        );

        workflowAction(
                document.getId(),
                "submit",
                tokenFor(collaborator)
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

        assertEquals(
                DocumentStatus.DRAFT,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldReturnConflictForInvalidLifecycleTransition()
            throws Exception {

        User manager = createUser(
                "workflow-conflict-manager",
                Role.MANAGER
        );

        Document document = createDocument(
                "Invalid approval",
                "Still a draft",
                manager,
                DocumentStatus.DRAFT
        );

        workflowAction(
                document.getId(),
                "approve",
                tokenFor(manager)
        )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("CONFLICT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid document status transition from DRAFT to APPROVED"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(
                                        "/api/v1/documents/"
                                                + document.getId()
                                                + "/approve"
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .exists()
                );

        assertEquals(
                DocumentStatus.DRAFT,
                reloadDocument(document.getId())
                        .getStatus()
        );
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist()
            throws Exception {

        User manager = createUser(
                "workflow-not-found",
                Role.MANAGER
        );

        UUID documentId = UUID.fromString(
                "99999999-9999-9999-9999-999999999999"
        );

        workflowAction(
                documentId,
                "approve",
                tokenFor(manager)
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
                                                + "/approve"
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .exists()
                );
    }

    @Test
    void shouldRejectUnauthenticatedWorkflowAction()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/documents/{id}/submit",
                                UUID.randomUUID()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    private ResultActions workflowAction(
            UUID documentId,
            String action,
            String token
    ) throws Exception {

        return mockMvc.perform(
                post(
                        "/api/v1/documents/{id}/{action}",
                        documentId,
                        action
                )
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
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
        Instant now = Instant.now();

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