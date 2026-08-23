package io.github.acarolinebcosta.docfy.auth.security;

import io.github.acarolinebcosta.docfy.auth.api.LoginRequest;
import io.github.acarolinebcosta.docfy.auth.api.LoginResponse;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(
        AuthenticationFlowIntegrationTest
                .ProtectedEndpointConfiguration.class
)
class AuthenticationFlowIntegrationTest
        implements PostgresTestContainer {

    private static final String RAW_PASSWORD =
            "docfy-integration-test-password";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAuthenticateThroughLoginAndAccessProtectedEndpoint()
            throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@docfy.local";
        User user = userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(RAW_PASSWORD),
                        Role.COLLABORATOR
                )
        );

        try {
            HttpResponse<String> loginResponse = login(email);

            assertEquals(200, loginResponse.statusCode());

            LoginResponse login = objectMapper.readValue(
                    loginResponse.body(),
                    LoginResponse.class
            );

            assertNotNull(login.accessToken());
            assertFalse(login.accessToken().isBlank());
            assertEquals("Bearer", login.tokenType());
            assertEquals(3600L, login.expiresIn());

            HttpResponse<String> protectedResponse = getProtected(
                    login.accessToken()
            );

            assertEquals(200, protectedResponse.statusCode());
            assertEquals("authenticated", protectedResponse.body());
        } finally {
            userRepository.deleteById(user.getId());
        }
    }

    private HttpResponse<String> login(String email) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest(email, RAW_PASSWORD)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> getProtected(String token)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/test/authentication-flow"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
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

        @GetMapping("/test/authentication-flow")
        String protectedEndpoint() {
            return "authenticated";
        }
    }
}
