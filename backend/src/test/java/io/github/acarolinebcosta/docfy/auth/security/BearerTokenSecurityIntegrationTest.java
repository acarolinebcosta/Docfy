package io.github.acarolinebcosta.docfy.auth.security;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.shared.error.ApiErrorResponse;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(
        BearerTokenSecurityIntegrationTest
                .ProtectedEndpointConfiguration.class
)
@Transactional
class BearerTokenSecurityIntegrationTest
        implements PostgresTestContainer {

    private static final String DIFFERENT_SECRET =
            "different-docfy-test-secret-key-with-at-least-32-bytes";
    private static final String CORRELATION_ID =
            "22222222-2222-2222-2222-222222222222";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        HttpResponse<String> response = getProtected(
                null,
                CORRELATION_ID
        );

        assertEquals(401, response.statusCode());
        assertTrue(
                response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .startsWith("application/json")
        );
        assertEquals(
                CORRELATION_ID,
                response.headers()
                        .firstValue(CorrelationIdFilter.HEADER_NAME)
                        .orElse(null)
        );

        ApiErrorResponse error = objectMapper.readValue(
                response.body(),
                ApiErrorResponse.class
        );

        assertNotNull(error.timestamp());
        assertEquals(401, error.status());
        assertEquals("UNAUTHORIZED", error.error());
        assertEquals("Authentication required", error.message());
        assertEquals("/test/protected", error.path());
        assertEquals(CORRELATION_ID, error.correlationId());
        assertFalse(response.body().contains("stackTrace"));
    }

    @Test
    void shouldAuthenticateProtectedEndpointWithValidToken()
            throws Exception {
        String token = jwtTokenService.generateToken(createUser());

        HttpResponse<String> response = getProtected(token);

        assertEquals(200, response.statusCode());
        assertEquals("protected", response.body());
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        HttpResponse<String> response = getProtected("not-a-jwt");

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        JwtProperties differentProperties = new JwtProperties(
                DIFFERENT_SECRET,
                jwtProperties.issuer(),
                jwtProperties.expirationSeconds()
        );

        JwtConfig differentConfig = new JwtConfig();
        JwtTokenService differentTokenService =
                new JwtTokenService(
                        differentConfig.jwtEncoder(differentProperties),
                        differentProperties
                );

        String token = differentTokenService.generateToken(createUser());

        HttpResponse<String> response = getProtected(token);

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        User user = createUser();
        Instant issuedAt = Instant.now().minusSeconds(300);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        HttpResponse<String> response = getProtected(token);

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldKeepLoginPublicWithoutToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                """
                                {
                                  "email": "",
                                  "password": ""
                                }
                                """
                        )
                )
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(400, response.statusCode());
    }

    private HttpResponse<String> getProtected(String token)
            throws Exception {
        return getProtected(token, null);
    }

    private HttpResponse<String> getProtected(
            String token,
            String correlationId
    ) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(uri("/test/protected"))
                .GET();

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        if (correlationId != null) {
            request.header(
                    CorrelationIdFilter.HEADER_NAME,
                    correlationId
            );
        }

        return client.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private User createUser() {
        User user = new User(
                "security-" + UUID.randomUUID() + "@docfy.local",
                "{noop}unused-password",
                Role.ADMIN
        );

        return userRepository.saveAndFlush(user);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProtectedEndpointConfiguration {

        @Bean
        ProtectedEndpointController protectedEndpointController() {
            return new ProtectedEndpointController();
        }
    }

    @RestController
    static class ProtectedEndpointController {

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }
    }
}
