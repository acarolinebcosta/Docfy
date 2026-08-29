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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentListingIntegrationTest
        implements PostgresTestContainer {

    private static final int DEFAULT_SIZE = 20;
    private static final Instant TEST_UPDATED_AT =
            Instant.parse("2100-01-01T00:00:00Z");

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
    void shouldRejectUnauthenticatedListing() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnEmptyPageOutsideVisibleResultSet()
            throws Exception {
        User viewer = createUser("listing-empty", Role.COLLABORATOR);
        long visibleBefore = visibleCount(viewer);
        int emptyPage = Math.toIntExact(
                visibleBefore / DEFAULT_SIZE + 1
        );

        mockMvc.perform(
                        listRequest(viewer)
                                .param("page", String.valueOf(emptyPage))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(emptyPage))
                .andExpect(jsonPath("$.size").value(DEFAULT_SIZE))
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(visibleBefore)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(totalPages(visibleBefore, DEFAULT_SIZE))
                );
    }

    @Test
    void shouldReturnOneNewDocumentWithoutInternalUserData()
            throws Exception {
        User viewer = createUser("listing-one", Role.COLLABORATOR);
        long visibleBefore = visibleCount(viewer);
        Document document = createDocument(
                uniqueTitle("Single visible document"),
                viewer,
                DocumentStatus.DRAFT,
                TEST_UPDATED_AT
        );

        mockMvc.perform(
                        listRequest(viewer)
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(
                        jsonPath("$.items[0].id")
                                .value(document.getId().toString())
                )
                .andExpect(
                        jsonPath("$.items[0].createdBy")
                                .value(viewer.getId().toString())
                )
                .andExpect(
                        jsonPath("$.items[0].passwordHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.items[0].email")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(visibleBefore + 1)
                );
    }

    @Test
    void shouldAllowAdminToListEveryDocument() throws Exception {
        User admin = createUser("listing-admin", Role.ADMIN);
        User owner = createUser("listing-admin-owner", Role.COLLABORATOR);
        long visibleBefore = visibleCount(admin);
        String prefix = uniqueTitle("Admin visibility");

        createDocumentsInEveryStatus(prefix, owner);

        mockMvc.perform(
                        listRequest(admin)
                                .param("size", "100")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(visibleBefore + 4)
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(
                                        hasItems(
                                                prefix + " DRAFT",
                                                prefix + " IN_REVIEW",
                                                prefix + " APPROVED",
                                                prefix + " ARCHIVED"
                                        )
                                )
                );
    }

    @Test
    void shouldAllowManagerToListEveryDocument() throws Exception {
        User manager = createUser("listing-manager", Role.MANAGER);
        User owner = createUser(
                "listing-manager-owner",
                Role.COLLABORATOR
        );
        long visibleBefore = visibleCount(manager);
        String prefix = uniqueTitle("Manager visibility");

        createDocumentsInEveryStatus(prefix, owner);

        mockMvc.perform(
                        listRequest(manager)
                                .param("size", "100")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(visibleBefore + 4)
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(prefix + " DRAFT"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(prefix + " IN_REVIEW"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(prefix + " APPROVED"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(prefix + " ARCHIVED"))
                );
    }

    @Test
    void shouldListOnlyOwnOrApprovedDocumentsForCollaborator()
            throws Exception {
        User viewer = createUser(
                "listing-collaborator",
                Role.COLLABORATOR
        );
        User otherUser = createUser(
                "listing-collaborator-other",
                Role.COLLABORATOR
        );
        long visibleBefore = visibleCount(viewer);
        String ownPrefix = uniqueTitle("Own visibility");
        String otherPrefix = uniqueTitle("Other visibility");

        createDocumentsInEveryStatus(ownPrefix, viewer);
        createDocumentsInEveryStatus(otherPrefix, otherUser);

        mockMvc.perform(
                        listRequest(viewer)
                                .param("size", "100")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(visibleBefore + 5)
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(ownPrefix + " DRAFT"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(ownPrefix + " IN_REVIEW"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(ownPrefix + " APPROVED"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(ownPrefix + " ARCHIVED"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(hasItem(otherPrefix + " APPROVED"))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(not(hasItem(otherPrefix + " DRAFT")))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(
                                        not(
                                                hasItem(
                                                        otherPrefix
                                                                + " IN_REVIEW"
                                                )
                                        )
                                )
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(
                                        not(
                                                hasItem(
                                                        otherPrefix
                                                                + " ARCHIVED"
                                                )
                                        )
                                )
                );
    }

    @Test
    void shouldPaginateAfterAuthorizationFiltering()
            throws Exception {
        User viewer = createUser(
                "listing-pagination",
                Role.COLLABORATOR
        );
        User otherUser = createUser(
                "listing-pagination-other",
                Role.COLLABORATOR
        );
        long visibleBefore = visibleCount(viewer);
        String prefix = uniqueTitle("Visible page");

        for (int index = 1; index <= 5; index++) {
            createDocument(
                    prefix + " " + index,
                    viewer,
                    DocumentStatus.DRAFT,
                    TEST_UPDATED_AT.plusSeconds(index)
            );
        }

        String concealedTitle = uniqueTitle("Concealed newest");
        createDocument(
                concealedTitle,
                otherUser,
                DocumentStatus.DRAFT,
                TEST_UPDATED_AT.plusSeconds(100)
        );

        long expectedTotal = visibleBefore + 5;

        mockMvc.perform(
                        listRequest(viewer)
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(expectedTotal)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(totalPages(expectedTotal, 2))
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(
                                        contains(
                                                prefix + " 5",
                                                prefix + " 4"
                                        )
                                )
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(not(hasItem(concealedTitle)))
                );

        mockMvc.perform(
                        listRequest(viewer)
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(expectedTotal)
                )
                .andExpect(
                        jsonPath("$.items[*].title")
                                .value(
                                        contains(
                                                prefix + " 3",
                                                prefix + " 2"
                                        )
                                )
                );
    }

    @Test
    void shouldUseIdAscendingAsDeterministicTieBreaker()
            throws Exception {
        User viewer = createUser(
                "listing-order",
                Role.COLLABORATOR
        );
        Document first = createDocument(
                uniqueTitle("Same timestamp A"),
                viewer,
                DocumentStatus.DRAFT,
                TEST_UPDATED_AT.plusSeconds(200)
        );
        Document second = createDocument(
                uniqueTitle("Same timestamp B"),
                viewer,
                DocumentStatus.DRAFT,
                TEST_UPDATED_AT.plusSeconds(200)
        );
        Document older = createDocument(
                uniqueTitle("Older"),
                viewer,
                DocumentStatus.DRAFT,
                TEST_UPDATED_AT.plusSeconds(199)
        );
        List<String> tiedIds = List.of(
                        first.getId().toString(),
                        second.getId().toString()
                )
                .stream()
                .sorted()
                .toList();

        mockMvc.perform(
                        listRequest(viewer)
                                .param("size", "3")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[*].id")
                                .value(
                                        contains(
                                                tiedIds.get(0),
                                                tiedIds.get(1),
                                                older.getId().toString()
                                        )
                                )
                );
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        User viewer = createUser(
                "listing-invalid-page",
                Role.COLLABORATOR
        );

        mockMvc.perform(
                        listRequest(viewer)
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Page must be greater than or equal to 0"
                                )
                );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void shouldRejectInvalidPageSize(int size) throws Exception {
        User viewer = createUser(
                "listing-invalid-size-" + size,
                Role.COLLABORATOR
        );

        mockMvc.perform(
                        listRequest(viewer)
                                .param("size", String.valueOf(size))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Size must be between 1 and 100")
                );
    }

    private MockHttpServletRequestBuilder listRequest(User viewer) {
        return get("/api/v1/documents")
                .header(
                        "Authorization",
                        "Bearer " + tokenFor(viewer)
                );
    }

    private void createDocumentsInEveryStatus(
            String titlePrefix,
            User creator
    ) {
        int index = 0;

        for (DocumentStatus status : DocumentStatus.values()) {
            createDocument(
                    titlePrefix + " " + status.name(),
                    creator,
                    status,
                    TEST_UPDATED_AT.plusSeconds(index++)
            );
        }
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
            DocumentStatus status,
            Instant updatedAt
    ) {
        User managedCreator = userRepository
                .findById(creator.getId())
                .orElseThrow();
        Document document = documentRepository.saveAndFlush(
                new Document(
                        title,
                        "Document listing integration test",
                        managedCreator
                )
        );

        jdbcTemplate.update(
                """
                UPDATE documents
                SET status = ?, updated_at = ?
                WHERE id = ?
                """,
                status.name(),
                Timestamp.from(updatedAt),
                document.getId()
        );

        entityManager.clear();

        return documentRepository
                .findById(document.getId())
                .orElseThrow();
    }

    private long visibleCount(User viewer) {
        if (viewer.getRole() == Role.ADMIN
                || viewer.getRole() == Role.MANAGER) {
            return documentRepository.count();
        }

        return documentRepository.findByCreatedByIdOrStatus(
                viewer.getId(),
                DocumentStatus.APPROVED,
                PageRequest.of(0, 1)
        ).getTotalElements();
    }

    private int totalPages(long totalElements, int size) {
        return Math.toIntExact((totalElements + size - 1) / size);
    }

    private String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID();
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
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}
