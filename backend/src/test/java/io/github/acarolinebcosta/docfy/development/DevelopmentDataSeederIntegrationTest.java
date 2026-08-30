package io.github.acarolinebcosta.docfy.development;

import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditAction;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEvent;
import io.github.acarolinebcosta.docfy.audit.domain.DocumentAuditEventRepository;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.document.domain.DocumentStatus;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = "DOCFY_DEV_SEED_PASSWORD=development-seed-test-password"
)
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DevelopmentDataSeederIntegrationTest
        implements PostgresTestContainer {

    private static final String SEED_PASSWORD =
            "development-seed-test-password";

    private static final Map<String, Role> EXPECTED_USERS = Map.of(
            "admin@docfy.local", Role.ADMIN,
            "manager@docfy.local", Role.MANAGER,
            "ana@docfy.local", Role.COLLABORATOR,
            "joao@docfy.local", Role.COLLABORATOR
    );

    private static final List<ExpectedDocument> EXPECTED_DOCUMENTS =
            List.of(
                    new ExpectedDocument(
                            "ana@docfy.local",
                            "Quality Policy",
                            DocumentStatus.DRAFT
                    ),
                    new ExpectedDocument(
                            "ana@docfy.local",
                            "Information Security Policy",
                            DocumentStatus.IN_REVIEW
                    ),
                    new ExpectedDocument(
                            "ana@docfy.local",
                            "Software Release Checklist",
                            DocumentStatus.APPROVED
                    ),
                    new ExpectedDocument(
                            "ana@docfy.local",
                            "Operational Procedure",
                            DocumentStatus.ARCHIVED
                    ),
                    new ExpectedDocument(
                            "joao@docfy.local",
                            "Architecture Guidelines",
                            DocumentStatus.DRAFT
                    ),
                    new ExpectedDocument(
                            "joao@docfy.local",
                            "Incident Response Procedure",
                            DocumentStatus.IN_REVIEW
                    ),
                    new ExpectedDocument(
                            "joao@docfy.local",
                            "Supplier Agreement",
                            DocumentStatus.APPROVED
                    ),
                    new ExpectedDocument(
                            "joao@docfy.local",
                            "Meeting Minutes",
                            DocumentStatus.ARCHIVED
                    )
            );

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DevelopmentSeedService seedService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentAuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldLoadDevelopmentSeedBeansInDevProfile() {
        assertFalse(
                applicationContext
                        .getBeansOfType(DevelopmentDataSeeder.class)
                        .isEmpty()
        );

        assertFalse(
                applicationContext
                        .getBeansOfType(DevelopmentSeedService.class)
                        .isEmpty()
        );
    }

    @Test
    void shouldCreateExpectedUsersWithEncodedPassword() {
        EXPECTED_USERS.forEach((email, role) -> {
            User user = seedUser(email);

            assertEquals(role, user.getRole());
            assertTrue(user.isActive());

            assertNotEquals(
                    SEED_PASSWORD,
                    user.getPasswordHash()
            );

            assertTrue(
                    passwordEncoder.matches(
                            SEED_PASSWORD,
                            user.getPasswordHash()
                    )
            );
        });
    }

    @Test
    @Transactional(readOnly = true)
    void shouldCreateExpectedDocumentsWithOwnersAndStatuses() {
        for (ExpectedDocument expected : EXPECTED_DOCUMENTS) {
            User owner = seedUser(expected.ownerEmail());
            Document document = seedDocument(expected);

            assertEquals(expected.title(), document.getTitle());
            assertEquals(expected.status(), document.getStatus());

            assertEquals(
                    owner.getId(),
                    document.getCreatedBy().getId()
            );
        }

        assertEquals(8, seedDocumentCount());
    }

    @Test
    @Transactional(readOnly = true)
    void shouldCreateCoherentWorkflowAuditHistory() {
        assertHistory("Quality Policy");

        assertHistory(
                "Information Security Policy",
                submittedBy("ana@docfy.local"),
                event(
                        "manager@docfy.local",
                        DocumentAuditAction.DOCUMENT_REJECTED,
                        DocumentStatus.IN_REVIEW,
                        DocumentStatus.DRAFT
                ),
                submittedBy("ana@docfy.local")
        );

        assertHistory(
                "Software Release Checklist",
                submittedBy("ana@docfy.local"),
                approvedByManager()
        );

        assertHistory(
                "Operational Procedure",
                submittedBy("ana@docfy.local"),
                approvedByManager(),
                archivedByManager()
        );

        assertHistory("Architecture Guidelines");

        assertHistory(
                "Incident Response Procedure",
                submittedBy("joao@docfy.local")
        );

        assertHistory(
                "Supplier Agreement",
                submittedBy("joao@docfy.local"),
                approvedByManager()
        );

        assertHistory(
                "Meeting Minutes",
                submittedBy("joao@docfy.local"),
                approvedByManager(),
                archivedByManager()
        );
    }

    @Test
    void shouldNotDuplicateSeedDataOrAuditEvents() {
        SeedCounts before = seedCounts();

        assertFalse(seedService.initialize(SEED_PASSWORD));
        assertFalse(seedService.initialize(SEED_PASSWORD));

        assertEquals(before, seedCounts());
        assertEquals(new SeedCounts(4, 8, 14), before);
    }

    @AfterAll
    void cleanUpSeedData() {
        List<Document> documents = seedDocuments();

        for (Document document : documents) {
            List<DocumentAuditEvent> events =
                    auditEventRepository
                            .findByDocumentIdOrderByOccurredAtAscIdAsc(
                                    document.getId()
                            );

            auditEventRepository.deleteAll(events);
        }

        auditEventRepository.flush();

        documentRepository.deleteAll(documents);
        documentRepository.flush();

        List<User> users = EXPECTED_USERS.keySet().stream()
                .map(this::seedUser)
                .toList();

        userRepository.deleteAll(users);
        userRepository.flush();
    }

    private User seedUser(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private Document seedDocument(ExpectedDocument expected) {
        User owner = seedUser(expected.ownerEmail());

        List<Document> documents = documentRepository.findAll().stream()
                .filter(document ->
                        document.getCreatedBy()
                                .getId()
                                .equals(owner.getId())
                )
                .filter(document ->
                        document.getTitle().equals(expected.title())
                )
                .toList();

        assertEquals(1, documents.size());

        return documents.getFirst();
    }

    private Document seedDocument(String title) {
        ExpectedDocument expected = EXPECTED_DOCUMENTS.stream()
                .filter(document ->
                        document.title().equals(title)
                )
                .findFirst()
                .orElseThrow();

        return seedDocument(expected);
    }

    private List<Document> seedDocuments() {
        List<User> seedUsers = EXPECTED_USERS.keySet().stream()
                .map(this::seedUser)
                .toList();

        return documentRepository.findAll().stream()
                .filter(document ->
                        seedUsers.stream().anyMatch(user ->
                                user.getId().equals(
                                        document.getCreatedBy().getId()
                                )
                        )
                )
                .toList();
    }

    private long seedDocumentCount() {
        return seedDocuments().size();
    }

    private long seedAuditEventCount() {
        return seedDocuments().stream()
                .mapToLong(document ->
                        auditEventRepository
                                .findByDocumentIdOrderByOccurredAtAscIdAsc(
                                        document.getId()
                                )
                                .size()
                )
                .sum();
    }

    private SeedCounts seedCounts() {
        long users = EXPECTED_USERS.keySet().stream()
                .filter(email ->
                        userRepository.findByEmail(email).isPresent()
                )
                .count();

        return new SeedCounts(
                users,
                seedDocumentCount(),
                seedAuditEventCount()
        );
    }

    private void assertHistory(
            String documentTitle,
            ExpectedAuditEvent... expectedEvents
    ) {
        Document document = seedDocument(documentTitle);

        List<DocumentAuditEvent> events =
                auditEventRepository
                        .findByDocumentIdOrderByOccurredAtAscIdAsc(
                                document.getId()
                        );

        assertEquals(expectedEvents.length, events.size());

        for (int index = 0; index < expectedEvents.length; index++) {
            DocumentAuditEvent actual = events.get(index);
            ExpectedAuditEvent expected = expectedEvents[index];

            assertEquals(
                    expected.actorEmail(),
                    actual.getActor().getEmail()
            );

            assertEquals(
                    expected.action(),
                    actual.getAction()
            );

            assertEquals(
                    expected.previousStatus(),
                    actual.getPreviousStatus()
            );

            assertEquals(
                    expected.newStatus(),
                    actual.getNewStatus()
            );

            assertNull(actual.getCorrelationId());
        }
    }

    private ExpectedAuditEvent submittedBy(String actorEmail) {
        return event(
                actorEmail,
                DocumentAuditAction.DOCUMENT_SUBMITTED,
                DocumentStatus.DRAFT,
                DocumentStatus.IN_REVIEW
        );
    }

    private ExpectedAuditEvent approvedByManager() {
        return event(
                "manager@docfy.local",
                DocumentAuditAction.DOCUMENT_APPROVED,
                DocumentStatus.IN_REVIEW,
                DocumentStatus.APPROVED
        );
    }

    private ExpectedAuditEvent archivedByManager() {
        return event(
                "manager@docfy.local",
                DocumentAuditAction.DOCUMENT_ARCHIVED,
                DocumentStatus.APPROVED,
                DocumentStatus.ARCHIVED
        );
    }

    private ExpectedAuditEvent event(
            String actorEmail,
            DocumentAuditAction action,
            DocumentStatus previousStatus,
            DocumentStatus newStatus
    ) {
        return new ExpectedAuditEvent(
                actorEmail,
                action,
                previousStatus,
                newStatus
        );
    }

    private record ExpectedDocument(
            String ownerEmail,
            String title,
            DocumentStatus status
    ) {
    }

    private record ExpectedAuditEvent(
            String actorEmail,
            DocumentAuditAction action,
            DocumentStatus previousStatus,
            DocumentStatus newStatus
    ) {
    }

    private record SeedCounts(
            long users,
            long documents,
            long auditEvents
    ) {
    }
}