package io.github.acarolinebcosta.docfy.shared.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
        };

        filter.doFilter(request, response, filterChain);

        String correlationId =
                response.getHeader(CorrelationIdFilter.HEADER_NAME);

        assertNotNull(correlationId);
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
    }

    @Test
    void shouldPreserveValidIncomingCorrelationId() throws Exception {
        String expectedCorrelationId =
                "11111111-1111-1111-1111-111111111111";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                expectedCorrelationId
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
        };

        filter.doFilter(request, response, filterChain);

        assertEquals(
                expectedCorrelationId,
                response.getHeader(CorrelationIdFilter.HEADER_NAME)
        );
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenIncomingHeaderIsInvalid() throws Exception {
        String invalidCorrelationId = "invalid-correlation-id";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                invalidCorrelationId
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
        };

        filter.doFilter(request, response, filterChain);

        String generatedCorrelationId =
                response.getHeader(CorrelationIdFilter.HEADER_NAME);

        assertNotNull(generatedCorrelationId);
        assertNotEquals(invalidCorrelationId, generatedCorrelationId);
        assertDoesNotThrow(() -> UUID.fromString(generatedCorrelationId));
    }
}
