package io.github.acarolinebcosta.docfy.category.api;

import jakarta.persistence.EntityManager;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.document.domain.Document;
import io.github.acarolinebcosta.docfy.document.domain.DocumentRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerIntegrationTest
        implements PostgresTestContainer {

    private static final UUID CONTRACT_CATEGORY_ID = UUID.fromString(
            "11111111-0000-0000-0000-000000000003"
    );

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
    void shouldListReferenceCategoriesAlphabetically()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);

        mockMvc.perform(
                        get("/api/v1/categories")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].name").value("Certificate"))
                .andExpect(jsonPath("$[1].name").value("Contract"))
                .andExpect(
                        jsonPath("$[2].name")
                                .value("Meeting Minutes")
                )
                .andExpect(jsonPath("$[6].name").value("Regulation"));
    }

    @Test
    void shouldRequireAuthenticationToListCategories()
            throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateDocumentWithSelectedCategory()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);

        mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Supplier contract",
                                          "description": "Contract terms",
                                          "categoryId": "11111111-0000-0000-0000-000000000003"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.category.id")
                                .value(CONTRACT_CATEGORY_ID.toString())
                )
                .andExpect(
                        jsonPath("$.category.name")
                                .value("Contract")
                );
    }

    @Test
    void shouldRejectCreationWithoutCategory()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);

        mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Unclassified document"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnknownCategoryDuringCreation()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);

        mockMvc.perform(
                        post("/api/v1/documents")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Unknown category",
                                          "categoryId": "99999999-9999-9999-9999-999999999999"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Category not found")
                );
    }

    @Test
    void shouldUpdateDraftCategory()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);
        Document document = documentRepository.saveAndFlush(
                new Document(
                        "Draft category",
                        null,
                        user
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/documents/{id}",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryId": "11111111-0000-0000-0000-000000000003"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.category.name")
                                .value("Contract")
                );
    }

    @Test
    void shouldRejectNullCategoryUpdate()
            throws Exception {
        User user = createUser(Role.COLLABORATOR);
        Document document = documentRepository.saveAndFlush(
                new Document("Draft category", null, user)
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/documents/{id}",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Category must not be null")
                );
    }

    @Test
    void shouldRejectCategoryUpdateOutsideDraft()
            throws Exception {
        User manager = createUser(Role.MANAGER);
        Document document = documentRepository.saveAndFlush(
                new Document("Reviewed category", null, manager)
        );
        jdbcTemplate.update(
                "UPDATE documents SET status = 'IN_REVIEW' WHERE id = ?",
                document.getId()
        );
        entityManager.clear();

        mockMvc.perform(
                        patch(
                                "/api/v1/documents/{id}",
                                document.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(manager)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "categoryId": "11111111-0000-0000-0000-000000000003"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    private User createUser(Role role) {
        return userRepository.save(
                new User(
                        "category-" + UUID.randomUUID()
                                + "@docfy.local",
                        passwordEncoder.encode("StrongPassword123!"),
                        role
                )
        );
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
