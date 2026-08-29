package io.github.acarolinebcosta.docfy.document.api;

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
                                          "description": "Document quality strategy"
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
                                          "description": "Invalid"
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
                  "description": "Invalid"
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
                                          "description": "Document quality strategy"
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

        mockMvc.perform(
                        get(
                                "/api/v1/documents/{id}",
                                documentId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenFor(user)
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
}
