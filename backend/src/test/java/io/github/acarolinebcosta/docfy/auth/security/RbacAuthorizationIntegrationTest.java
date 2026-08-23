package io.github.acarolinebcosta.docfy.auth.security;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.shared.error.ApiErrorResponse;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import io.github.acarolinebcosta.docfy.shared.security.ApiAccessDeniedHandler;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(
        RbacAuthorizationIntegrationTest
                .RbacEndpointConfiguration.class
)
@Transactional
class RbacAuthorizationIntegrationTest
        implements PostgresTestContainer {

    private static final String CORRELATION_ID =
            "11111111-1111-1111-1111-111111111111";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAllowAdminToAccessAdminEndpoint() throws Exception {
        HttpResponse<String> response = get(
                "/test/admin",
                Role.ADMIN,
                null
        );

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldForbidManagerFromAdminEndpointWithStructuredResponse()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/admin",
                Role.MANAGER,
                CORRELATION_ID
        );

        assertEquals(403, response.statusCode());
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
        assertEquals(403, error.status());
        assertEquals("FORBIDDEN", error.error());
        assertEquals("Access denied", error.message());
        assertEquals("/test/admin", error.path());
        assertEquals(CORRELATION_ID, error.correlationId());
        assertFalse(response.body().contains("stackTrace"));
    }

    @Test
    void shouldForbidCollaboratorFromAdminEndpoint()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/admin",
                Role.COLLABORATOR,
                null
        );

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowAdminToAccessManagementEndpoint()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/management",
                Role.ADMIN,
                null
        );

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldAllowManagerToAccessManagementEndpoint()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/management",
                Role.MANAGER,
                null
        );

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldForbidCollaboratorFromManagementEndpoint()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/management",
                Role.COLLABORATOR,
                null
        );

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowCollaboratorToAccessAuthenticatedEndpoint()
            throws Exception {
        HttpResponse<String> response = get(
                "/test/authenticated",
                Role.COLLABORATOR,
                null
        );

        assertEquals(200, response.statusCode());
    }

    private HttpResponse<String> get(
            String path,
            Role role,
            String correlationId
    ) throws Exception {
        String token = jwtTokenService.generateToken(createUser(role));

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET();

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

    private User createUser(Role role) {
        User user = new User(
                "rbac-" + UUID.randomUUID() + "@docfy.local",
                "{noop}unused-password",
                role
        );

        return userRepository.saveAndFlush(user);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RbacEndpointConfiguration {

        @Bean
        @Order(1)
        SecurityFilterChain rbacSecurityFilterChain(
                HttpSecurity http,
                JwtAuthenticationConverter jwtAuthenticationConverter,
                ApiAccessDeniedHandler accessDeniedHandler
        ) throws Exception {
            http
                    .securityMatcher(
                            "/test/admin",
                            "/test/management",
                            "/test/authenticated"
                    )
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.STATELESS
                            )
                    )
                    .authorizeHttpRequests(authorize ->
                            authorize
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/test/admin"
                                    )
                                    .hasRole("ADMIN")
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/test/management"
                                    )
                                    .hasAnyRole("ADMIN", "MANAGER")
                                    .anyRequest()
                                    .authenticated()
                    )
                    .exceptionHandling(exceptions ->
                            exceptions.accessDeniedHandler(
                                    accessDeniedHandler
                            )
                    )
                    .oauth2ResourceServer(resourceServer ->
                            resourceServer.jwt(jwt ->
                                    jwt.jwtAuthenticationConverter(
                                            jwtAuthenticationConverter
                                    )
                            )
                    );

            return http.build();
        }

        @Bean
        RbacEndpointController rbacEndpointController() {
            return new RbacEndpointController();
        }
    }

    @RestController
    static class RbacEndpointController {

        @GetMapping("/test/admin")
        String admin() {
            return "admin";
        }

        @GetMapping("/test/management")
        String management() {
            return "management";
        }

        @GetMapping("/test/authenticated")
        String authenticated() {
            return "authenticated";
        }
    }
}
