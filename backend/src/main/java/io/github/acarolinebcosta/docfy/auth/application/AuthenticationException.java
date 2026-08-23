package io.github.acarolinebcosta.docfy.auth.application;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("Invalid credentials");
    }
}