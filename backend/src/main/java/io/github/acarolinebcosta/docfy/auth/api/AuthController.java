package io.github.acarolinebcosta.docfy.auth.api;

import io.github.acarolinebcosta.docfy.auth.application.AuthenticationService;
import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.security.JwtProperties;
import io.github.acarolinebcosta.docfy.auth.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthController(
            AuthenticationService authenticationService,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties
    ) {
        this.authenticationService = authenticationService;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authenticationService.authenticate(
                request.email(),
                request.password()
        );

        String accessToken = jwtTokenService.generateToken(user);

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                jwtProperties.expirationSeconds()
        );

        return ResponseEntity.ok(response);
    }
}
