package io.github.acarolinebcosta.docfy.shared.health;

import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class HealthEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnUpHealthStatusWithCorrelationId() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                "http://localhost:"
                                        + port
                                        + "/actuator/health"
                        )
                )
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, response.statusCode());

        assertTrue(
                response.body().contains("\"status\":\"UP\"")
        );

        String correlationId = response
                .headers()
                .firstValue(CorrelationIdFilter.HEADER_NAME)
                .orElse(null);

        assertNotNull(correlationId);

        assertDoesNotThrow(
                () -> UUID.fromString(correlationId)
        );
    }
}