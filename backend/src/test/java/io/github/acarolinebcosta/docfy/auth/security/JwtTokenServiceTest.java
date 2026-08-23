package io.github.acarolinebcosta.docfy.auth.security;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    private static final String SECRET =
            "docfy-test-secret-key-with-at-least-32-bytes";

    @Test
    void shouldGenerateTokenWithExpectedClaims() {
        JwtProperties properties = new JwtProperties(
                SECRET,
                "docfy",
                3600
        );

        JwtConfig config = new JwtConfig();

        var encoder = config.jwtEncoder(properties);
        JwtDecoder decoder = config.jwtDecoder(properties);

        JwtTokenService service =
                new JwtTokenService(
                        encoder,
                        properties
                );

        UUID userId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("admin@docfy.local");
        when(user.getRole()).thenReturn(Role.ADMIN);

        String token = service.generateToken(user);

        assertNotNull(token);

        Jwt jwt = decoder.decode(token);

        assertEquals(
                userId.toString(),
                jwt.getSubject()
        );

        assertEquals(
            "docfy",
            jwt.getClaimAsString("iss")
        );

        assertEquals(
                "admin@docfy.local",
                jwt.getClaimAsString("email")
        );

        assertEquals(
                "ADMIN",
                jwt.getClaimAsString("role")
        );

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());

        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();

        assertTrue(expiresAt.isAfter(issuedAt));
    }
}