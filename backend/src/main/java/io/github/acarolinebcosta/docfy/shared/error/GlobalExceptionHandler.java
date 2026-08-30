package io.github.acarolinebcosta.docfy.shared.error;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.document.application.DocumentEditForbiddenException;
import io.github.acarolinebcosta.docfy.document.application.DocumentNotFoundException;
import io.github.acarolinebcosta.docfy.shared.error.exception.BadRequestException;
import io.github.acarolinebcosta.docfy.shared.observability.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentEditForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentEditForbiddenException(
            DocumentEditForbiddenException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Document edit forbidden. correlationId={}, path={}",
                correlationId,
                request.getRequestURI()
        );

        return buildResponseEntity(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Bad request. correlationId={}, path={}, message={}",
                correlationId,
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Request validation failed. correlationId={}, path={}",
                correlationId,
                request.getRequestURI()
        );

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Authentication failed. correlationId={}, path={}",
                correlationId,
                request.getRequestURI()
        );

        return buildResponseEntity(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "HTTP method not allowed. correlationId={}, path={}, method={}",
                correlationId,
                request.getRequestURI(),
                exception.getMethod()
        );

        ApiErrorResponse body = buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported",
                request.getRequestURI(),
                correlationId
        );

        HttpHeaders headers = new HttpHeaders();

        if (exception.getSupportedHttpMethods() != null) {
            headers.setAllow(exception.getSupportedHttpMethods());
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Unsupported media type. correlationId={}, path={}, contentType={}",
                correlationId,
                request.getRequestURI(),
                exception.getContentType()
        );

        return buildResponseEntity(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentNotFoundException(
            DocumentNotFoundException exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.warn(
                "Document not found. correlationId={}, path={}",
                correlationId,
                request.getRequestURI()
        );

        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                "Document not found",
                request.getRequestURI(),
                correlationId
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();

        LOGGER.error(
                "Unexpected application error. correlationId={}, path={}",
                correlationId,
                request.getRequestURI(),
                exception
        );

        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal server error",
                request.getRequestURI(),
                correlationId
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponseEntity(
            HttpStatus status,
            String message,
            String path,
            String correlationId
    ) {
        return ResponseEntity
                .status(status)
                .body(
                        buildResponse(
                                status,
                                message,
                                path,
                                correlationId
                        )
                );
    }

    private ApiErrorResponse buildResponse(
            HttpStatus status,
            String message,
            String path,
            String correlationId
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                message,
                path,
                correlationId
        );
    }

    private String getCorrelationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
