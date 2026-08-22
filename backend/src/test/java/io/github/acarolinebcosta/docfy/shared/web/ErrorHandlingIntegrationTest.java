package io.github.acarolinebcosta.docfy.shared.web;

import io.github.acarolinebcosta.docfy.shared.error.GlobalExceptionHandler;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import io.github.acarolinebcosta.docfy.shared.error.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlingIntegrationTest {

    private static final String CORRELATION_ID =
            "11111111-1111-1111-1111-111111111111";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void shouldReturnStructuredBadRequestWithCorrelationId() throws Exception {
        mockMvc.perform(
                        get("/test/bad-request")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.path").value("/test/bad-request"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void shouldReturnSafeInternalServerErrorWithCorrelationId() throws Exception {
        mockMvc.perform(
                        get("/test/internal-error")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isInternalServerError())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Unexpected internal server error")
                )
                .andExpect(jsonPath("$.path").value("/test/internal-error"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }
    @Test
void shouldReturnMethodNotAllowedWithCorrelationId() throws Exception {
    mockMvc.perform(
                    get("/test/post-only")
                            .header(
                                    CorrelationIdFilter.HEADER_NAME,
                                    CORRELATION_ID
                            )
            )
            .andExpect(status().isMethodNotAllowed())
            .andExpect(
                    header().string(
                            CorrelationIdFilter.HEADER_NAME,
                            CORRELATION_ID
                    )
            )
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
            .andExpect(
                    jsonPath("$.message")
                            .value("HTTP method not supported")
            )
            .andExpect(jsonPath("$.path").value("/test/post-only"))
            .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
}

@Test
void shouldReturnUnsupportedMediaTypeWithCorrelationId() throws Exception {
    mockMvc.perform(
                    post("/test/json-only")
                            .header(
                                    CorrelationIdFilter.HEADER_NAME,
                                    CORRELATION_ID
                            )
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("{}")
            )
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(
                    header().string(
                            CorrelationIdFilter.HEADER_NAME,
                            CORRELATION_ID
                    )
            )
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(
                    jsonPath("$.message")
                            .value("Unsupported media type")
            )
            .andExpect(jsonPath("$.path").value("/test/json-only"))
            .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
}
@Test
void shouldTreatUnexpectedIllegalArgumentAsInternalServerError() throws Exception {
    mockMvc.perform(
                    get("/test/unexpected-illegal-argument")
                            .header(
                                    CorrelationIdFilter.HEADER_NAME,
                                    CORRELATION_ID
                            )
            )
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
            .andExpect(
                    jsonPath("$.message")
                            .value("Unexpected internal server error")
            )
            .andExpect(
                    jsonPath("$.path")
                            .value("/test/unexpected-illegal-argument")
            )
            .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
}

    @RestController
    static class TestController {

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BadRequestException("Invalid request");
        }

        @GetMapping("/test/internal-error")
        void internalError() {
            throw new RuntimeException("Sensitive internal information");
        }
        @PostMapping("/test/post-only")
        void postOnly() {
        }

        @PostMapping(
                value = "/test/json-only",
                consumes = MediaType.APPLICATION_JSON_VALUE
        )
        void jsonOnly() {
        }
        @GetMapping("/test/unexpected-illegal-argument")
        void unexpectedIllegalArgument() {
        throw new IllegalArgumentException(
                "Sensitive internal implementation detail"
        );
        }
    }
}