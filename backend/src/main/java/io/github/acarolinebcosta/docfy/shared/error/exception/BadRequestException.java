package io.github.acarolinebcosta.docfy.shared.error.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}