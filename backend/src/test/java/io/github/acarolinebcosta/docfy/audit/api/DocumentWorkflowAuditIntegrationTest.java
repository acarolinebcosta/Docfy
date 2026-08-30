package io.github.acarolinebcosta.docfy.audit.api;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditAction;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEvent;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEventRepository;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentWorkflowAuditIntegrationTest
        implements PostgresTestContainer {

    private static final String CORRELATION_SUBMIT =
            "11111111-1111-1111-1111-111111111111";

    private static final String CORRELATION_REJECT =
            "22222222-2222-2222-2222-222222222222";

    private static final String CORRELATION_RESUBMIT =
            "33333333-3333-3333-3333-333333333333";

    private static final String CORRELATION_APPROVE =
            "44444444-4444-4444-4444-444444444444";

    private static final String CORRELATION_ARCHIVE =
            "55555555-5555-5555-5555-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentAuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistCompleteWorkflowAuditHistory()
            throws Exception {

        User manager = createUser(
                "audit-full-manager",
                Role.MANAGER
        );

        User owner = createUser(
                "audit-full-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Complete audit history",
                "Document used to validate the complete audit trail",
                owner,
                DocumentStatus.DRAFT
        );

        String token = tokenFor(manager);

        workflowAction(
                document.getId(),
                "submit",
                token,
                CORRELATION_SUBMIT
        ).andExpect(status().isOk());

        workflowAction(
                document.getId(),
                "reject",
                token,
                CORRELATION_REJECT
        ).andExpect(status().isOk());

        workflowAction(
                document.getId(),
                "submit",
                token,
                CORRELATION_RESUBMIT
        ).andExpect(status().isOk());

        workflowAction(
                document.getId(),
                "approve",
                token,
                CORRELATION_APPROVE
        ).andExpect(status().isOk());

        workflowAction(
                document.getId(),
                "archive",
                token,
                CORRELATION_ARCHIVE
        ).andExpect(status().isOk());

        Document persisted =
                reloadDocument(document.getId());

        assertEquals(
                DocumentStatus.ARCHIVED,
                persisted.getStatus()
        );

        List<DocumentAuditEvent> events =
                auditEvents(document.getId());

        assertEquals(5, events.size());

        assertEvent(
                events.get(0),
                manager,
                DocumentAuditAction.DOCUMENT_SUBMITTED,
                DocumentStatus.DRAFT,
                DocumentStatus.IN_REVIEW,
                CORRELATION_SUBMIT
        );

        assertEvent(
                events.get(1),
                manager,
                DocumentAuditAction.DOCUMENT_REJECTED,
                DocumentStatus.IN_REVIEW,
                DocumentStatus.DRAFT,
                CORRELATION_REJECT
        );

        assertEvent(
                events.get(2),
                manager,
                DocumentAuditAction.DOCUMENT_SUBMITTED,
                DocumentStatus.DRAFT,
                DocumentStatus.IN_REVIEW,
                CORRELATION_RESUBMIT
        );

        assertEvent(
                events.get(3),
                manager,
                DocumentAuditAction.DOCUMENT_APPROVED,
                DocumentStatus.IN_REVIEW,
                DocumentStatus.APPROVED,
                CORRELATION_APPROVE
        );

        assertEvent(
                events.get(4),
                manager,
                DocumentAuditAction.DOCUMENT_ARCHIVED,
                DocumentStatus.APPROVED,
                DocumentStatus.ARCHIVED,
                CORRELATION_ARCHIVE
        );
    }

    @Test
    void shouldRecordCollaboratorAsActorWhenSubmittingOwnDraft()
            throws Exception {

        User collaborator = createUser(
                "audit-own-submit",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Collaborator audited draft",
                "Own document submission",
                collaborator,
                DocumentStatus.DRAFT
        );

        String correlationId =
                "66666666-6666-6666-6666-666666666666";

        workflowAction(
                document.getId(),
                "submit",
                tokenFor(collaborator),
                correlationId
        ).andExpect(status().isOk());

        List<DocumentAuditEvent> events =
                auditEvents(document.getId());

        assertEquals(1, events.size());

        assertEvent(
                events.getFirst(),
                collaborator,
                DocumentAuditAction.DOCUMENT_SUBMITTED,
                DocumentStatus.DRAFT,
                DocumentStatus.IN_REVIEW,
                correlationId
        );
    }

    @Test
    void shouldNotCreateAuditWhenWorkflowActionIsForbidden()
            throws Exception {

        User collaborator = createUser(
                "audit-forbidden",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Forbidden approval",
                "Collaborator cannot approve",
                collaborator,
                DocumentStatus.IN_REVIEW
        );

        workflowAction(
                document.getId(),
                "approve",
                tokenFor(collaborator),
                "77777777-7777-7777-7777-777777777777"
        ).andExpect(status().isForbidden());

        assertEquals(
                DocumentStatus.IN_REVIEW,
                reloadDocument(document.getId()).getStatus()
        );

        assertTrue(
                auditEvents(document.getId()).isEmpty()
        );
    }

    @Test
    void shouldNotCreateAuditWhenLifecycleTransitionIsInvalid()
            throws Exception {

        User manager = createUser(
                "audit-invalid-transition",
                Role.MANAGER
        );

        Document document = createDocument(
                "Invalid audit transition",
                "Still a draft",
                manager,
                DocumentStatus.DRAFT
        );

        workflowAction(
                document.getId(),
                "approve",
                tokenFor(manager),
                "88888888-8888-8888-8888-888888888888"
        ).andExpect(status().isConflict());

        assertEquals(
                DocumentStatus.DRAFT,
                reloadDocument(document.getId()).getStatus()
        );

        assertTrue(
                auditEvents(document.getId()).isEmpty()
        );
    }

    @Test
    void shouldNotCreateAuditForConcealedDocument()
            throws Exception {

        User collaborator = createUser(
                "audit-hidden-viewer",
                Role.COLLABORATOR
        );

        User owner = createUser(
                "audit-hidden-owner",
                Role.COLLABORATOR
        );

        Document document = createDocument(
                "Hidden draft",
                "Other user's private draft",
                owner,
                DocumentStatus.DRAFT
        );

        workflowAction(
                document.getId(),
                "submit",
                tokenFor(collaborator),
                "99999999-9999-9999-9999-999999999999"
        ).andExpect(status().isNotFound());

        assertEquals(
                DocumentStatus.DRAFT,
                reloadDocument(document.getId()).getStatus()
        );

        assertTrue(
                auditEvents(document.getId()).isEmpty()
        );
    }

    private void assertEvent(
            DocumentAuditEvent event,
            User expectedActor,
            DocumentAuditAction expectedAction,
            DocumentStatus expectedPreviousStatus,
            DocumentStatus expectedNewStatus,
            String expectedCorrelationId
    ) {
        assertEquals(
                expectedActor.getId(),
                event.getActor().getId()
        );

        assertEquals(
                expectedAction,
                event.getAction()
        );

        assertEquals(
                expectedPreviousStatus,
                event.getPreviousStatus()
        );

        assertEquals(
                expectedNewStatus,
                event.getNewStatus()
        );

        assertEquals(
                expectedCorrelationId,
                event.getCorrelationId()
        );

        assertTrue(
                event.getOccurredAt() != null
        );
    }

    private List<DocumentAuditEvent> auditEvents(
            UUID documentId
    ) {
        entityManager.flush();

        return auditEventRepository
                .findByDocumentIdOrderByOccurredAtAscIdAsc(
                        documentId
                );
    }

    private ResultActions workflowAction(
            UUID documentId,
            String action,
            String token,
            String correlationId
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
                        .header(
                                CorrelationIdFilter.HEADER_NAME,
                                correlationId
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