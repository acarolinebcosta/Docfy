package io.github.acarolinebcosta.docfy.auth.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationService;
import io.github.acarolinebcosta.docfy.auth.application.AuthenticationException;
import io.github.acarolinebcosta.docfy.auth.domain.Role;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.security.JwtProperties;
import io.github.acarolinebcosta.docfy.auth.security.JwtTokenService;
import io.github.acarolinebcosta.docfy.shared.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthenticationService authenticationService;
    private JwtTokenService jwtTokenService;
    private JwtProperties jwtProperties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationService =
                mock(AuthenticationService.class);
        jwtTokenService = mock(JwtTokenService.class);
        jwtProperties = mock(JwtProperties.class);

        AuthController controller =
                new AuthController(
                        authenticationService,
                        jwtTokenService,
                        jwtProperties
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnJwtForValidCredentials()
            throws Exception {

        User user = new User(
                "admin@docfy.local",
                "{bcrypt}encoded-password",
                Role.ADMIN
        );

        when(
                authenticationService.authenticate(
                        "admin@docfy.local",
                        "StrongPassword123!"
                )
        ).thenReturn(user);

        when(jwtTokenService.generateToken(user))
                .thenReturn("generated.jwt.token");
        when(jwtProperties.expirationSeconds())
                .thenReturn(3600L);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "admin@docfy.local",
                                          "password": "StrongPassword123!"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("generated.jwt.token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(3600)
                );
    }

    @Test
    void shouldRejectInvalidLoginPayload()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "",
                                          "password": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
    @Test
    void shouldReturnUnauthorizedForInvalidCredentials()
            throws Exception {

        when(
                authenticationService.authenticate(
                        "admin@docfy.local",
                        "wrong-password"
                )
        ).thenThrow(new AuthenticationException());

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "email": "admin@docfy.local",
                                        "password": "wrong-password"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("UNAUTHORIZED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Invalid credentials")
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/v1/auth/login")
                );
    }
}
