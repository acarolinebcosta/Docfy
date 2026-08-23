package io.github.acarolinebcosta.docfy.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docfy.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long expirationSeconds
) {
}