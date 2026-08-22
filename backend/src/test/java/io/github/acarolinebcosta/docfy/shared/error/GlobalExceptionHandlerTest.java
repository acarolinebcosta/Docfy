package io.github.acarolinebcosta.docfy.shared.error;

import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import io.github.acarolinebcosta.docfy.shared.error.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID =
            "11111111-1111-1111-1111-111111111111";

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @AfterEach
    void cleanUpMdc() {
        MDC.clear();
    }

    @Test
    void shouldReturnStructuredBadRequestResponse() {
        MDC.put(
                CorrelationIdFilter.MDC_KEY,
                CORRELATION_ID
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/documents");

        BadRequestException exception =
            new BadRequestException("Invalid request");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleBadRequest(
                        exception,
                        request
                );

        assertEquals(400, response.getStatusCode().value());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());

        assertEquals(400, body.status());
        assertEquals("BAD_REQUEST", body.error());
        assertEquals("Invalid request", body.message());
        assertEquals("/api/v1/documents", body.path());
        assertEquals(CORRELATION_ID, body.correlationId());
    }

    @Test
    void shouldReturnStructuredInternalServerErrorResponse() {
        MDC.put(
                CorrelationIdFilter.MDC_KEY,
                CORRELATION_ID
        );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/documents");

        RuntimeException exception =
                new RuntimeException("Database password leaked here");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpectedError(
                        exception,
                        request
                );

        assertEquals(500, response.getStatusCode().value());

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertNotNull(body.timestamp());

        assertEquals(500, body.status());
        assertEquals("INTERNAL_SERVER_ERROR", body.error());

        assertEquals(
                "Unexpected internal server error",
                body.message()
        );

        assertEquals("/api/v1/documents", body.path());
        assertEquals(CORRELATION_ID, body.correlationId());
    }
}