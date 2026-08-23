package io.github.acarolinebcosta.docfy.auth.application;

import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authenticationService =
                new AuthenticationService(
                        userRepository,
                        passwordEncoder
                );
    }

    @Test
    void shouldAuthenticateValidActiveUser() {
        User user = new User(
                "admin@docfy.local",
                "{bcrypt}encoded-password",
                Role.ADMIN
        );

        when(
                userRepository.findByEmail("admin@docfy.local")
        ).thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        "StrongPassword123!",
                        "{bcrypt}encoded-password"
                )
        ).thenReturn(true);

        User authenticatedUser =
                authenticationService.authenticate(
                        "admin@docfy.local",
                        "StrongPassword123!"
                );

        assertSame(user, authenticatedUser);

        verify(passwordEncoder).matches(
                "StrongPassword123!",
                "{bcrypt}encoded-password"
        );
    }

    @Test
    void shouldRejectNonexistentUser() {
        when(
                userRepository.findByEmail("missing@docfy.local")
        ).thenReturn(Optional.empty());

        AuthenticationException exception =
                assertThrows(
                        AuthenticationException.class,
                        () -> authenticationService.authenticate(
                                "missing@docfy.local",
                                "StrongPassword123!"
                        )
                );

        assertEquals(
                "Invalid credentials",
                exception.getMessage()
        );

        verify(
                passwordEncoder,
                never()
        ).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = new User(
                "admin@docfy.local",
                "{bcrypt}encoded-password",
                Role.ADMIN
        );

        when(
                userRepository.findByEmail("admin@docfy.local")
        ).thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        "wrong-password",
                        "{bcrypt}encoded-password"
                )
        ).thenReturn(false);

        AuthenticationException exception =
                assertThrows(
                        AuthenticationException.class,
                        () -> authenticationService.authenticate(
                                "admin@docfy.local",
                                "wrong-password"
                        )
                );

        assertEquals(
                "Invalid credentials",
                exception.getMessage()
        );
    }
    @Test
void shouldRejectInactiveUser() {
    User user = User.inactive(
            "inactive@docfy.local",
            "{bcrypt}encoded-password",
            Role.COLLABORATOR
    );

    when(
            userRepository.findByEmail("inactive@docfy.local")
    ).thenReturn(Optional.of(user));

    AuthenticationException exception =
            assertThrows(
                    AuthenticationException.class,
                    () -> authenticationService.authenticate(
                            "inactive@docfy.local",
                            "StrongPassword123!"
                    )
            );

    assertEquals(
            "Invalid credentials",
            exception.getMessage()
    );

    verify(
            passwordEncoder,
            never()
    ).matches(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
    );
}
}