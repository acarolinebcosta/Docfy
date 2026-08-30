package io.github.acarolinebcosta.docfy.audit.api;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentAuditControllerIntegrationTest
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

    @Test
    void shouldReturnCompleteAuditHistoryToManager()
            throws Exception {

        User manager = createUser(
                "audit-query-manager",
                Role.MANAGER
        );

        User owner = createUser(
                "audit-query-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Audit history",
                owner
        );

        String token = tokenFor(manager);

        workflowAction(
                document.getId(),
                "submit",
                token,
                "11111111-1111-1111-1111-111111111111"
        );

        workflowAction(
                document.getId(),
                "reject",
                token,
                "22222222-2222-2222-2222-222222222222"
        );

        workflowAction(
                document.getId(),
                "submit",
                token,
                "33333333-3333-3333-3333-333333333333"
        );

        workflowAction(
                document.getId(),
                "approve",
                token,
                "44444444-4444-4444-4444-444444444444"
        );

        workflowAction(
                document.getId(),
                "archive",
                token,
                "55555555-5555-5555-5555-555555555555"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$", hasSize(5))
                )

                .andExpect(
                        jsonPath("$[0].documentId")
                                .value(document.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].actorId")
                                .value(manager.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].action")
                                .value("DOCUMENT_SUBMITTED")
                )
                .andExpect(
                        jsonPath("$[0].previousStatus")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$[0].newStatus")
                                .value("IN_REVIEW")
                )
                .andExpect(
                        jsonPath("$[0].correlationId")
                                .value(
                                        "11111111-1111-1111-1111-111111111111"
                                )
                )
                .andExpect(
                        jsonPath("$[0].occurredAt")
                                .exists()
                )

                .andExpect(
                        jsonPath("$[1].action")
                                .value("DOCUMENT_REJECTED")
                )
                .andExpect(
                        jsonPath("$[1].previousStatus")
                                .value("IN_REVIEW")
                )
                .andExpect(
                        jsonPath("$[1].newStatus")
                                .value("DRAFT")
                )

                .andExpect(
                        jsonPath("$[2].action")
                                .value("DOCUMENT_SUBMITTED")
                )
                .andExpect(
                        jsonPath("$[2].previousStatus")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$[2].newStatus")
                                .value("IN_REVIEW")
                )

                .andExpect(
                        jsonPath("$[3].action")
                                .value("DOCUMENT_APPROVED")
                )
                .andExpect(
                        jsonPath("$[3].previousStatus")
                                .value("IN_REVIEW")
                )
                .andExpect(
                        jsonPath("$[3].newStatus")
                                .value("APPROVED")
                )

                .andExpect(
                        jsonPath("$[4].action")
                                .value("DOCUMENT_ARCHIVED")
                )
                .andExpect(
                        jsonPath("$[4].previousStatus")
                                .value("APPROVED")
                )
                .andExpect(
                        jsonPath("$[4].newStatus")
                                .value("ARCHIVED")
                )
                .andExpect(
                        jsonPath("$[4].correlationId")
                                .value(
                                        "55555555-5555-5555-5555-555555555555"
                                )
                );
    }

    @Test
    void shouldAllowAdminToViewDocumentAudit()
            throws Exception {

        User manager = createUser(
                "audit-query-actor",
                Role.MANAGER
        );

        User admin = createUser(
                "audit-query-admin",
                Role.ADMIN
        );

        User owner = createUser(
                "audit-query-admin-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Admin audit access",
                owner
        );

        workflowAction(
                document.getId(),
                "submit",
                tokenFor(manager),
                "66666666-6666-6666-6666-666666666666"
        );

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(admin)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$", hasSize(1))
                )
                .andExpect(
                        jsonPath("$[0].actorId")
                                .value(manager.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].action")
                                .value("DOCUMENT_SUBMITTED")
                );
    }

    @Test
    void shouldReturnForbiddenWhenCollaboratorRequestsVisibleAudit()
            throws Exception {

        User collaborator = createUser(
                "audit-query-forbidden",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Own visible document",
                collaborator
        );

        String correlationId =
                "77777777-7777-7777-7777-777777777777";

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + tokenFor(collaborator)
                                )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        correlationId
                                )
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
                                                + "/audit"
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(correlationId)
                );
    }

    @Test
    void shouldConcealAuditWhenCollaboratorCannotViewDocument()
            throws Exception {

        User collaborator = createUser(
                "audit-query-hidden-viewer",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "audit-query-hidden-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Other user's private draft",
                owner
        );

        String correlationId =
                "88888888-8888-8888-8888-888888888888";

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + tokenFor(collaborator)
                                )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        correlationId
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
                        jsonPath("$.correlationId")
                                .value(correlationId)
                );
    }

    @Test
    void shouldReturnNotFoundWhenAuditDocumentDoesNotExist()
            throws Exception {

        User manager = createUser(
                "audit-query-not-found",
                Role.MANAGER
        );

        UUID documentId =
                UUID.fromString(
                        "99999999-9999-9999-9999-999999999999"
                );

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                documentId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(manager)
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Document not found")
                );
    }

    @Test
    void shouldRejectUnauthenticatedAuditRequest()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}/audit",
                                UUID.randomUUID()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    private void workflowAction(
            UUID documentId,
            String action,
            String token,
            String correlationId
    ) throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/documents/{id}/{action}",
                                documentId,
                                action
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        correlationId
                                )
                )
                .andExpect(status().isOk());
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
            User creator
    ) {
        User managedCreator =
                userRepository
                        .findById(creator.getId())
                        .orElseThrow();

        return documentRepository.saveAndFlush(
                new Document(
                        title,
                        "Audit query integration test",
                        managedCreator
                )
        );
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