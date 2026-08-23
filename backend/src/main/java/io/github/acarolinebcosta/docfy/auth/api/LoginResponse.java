package io.github.acarolinebcosta.docfy.auth.api;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}