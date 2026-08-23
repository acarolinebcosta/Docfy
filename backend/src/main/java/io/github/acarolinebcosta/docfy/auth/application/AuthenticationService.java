package io.github.acarolinebcosta.docfy.auth.application;

import io.github.acarolinebcosta.docfy.auth.domain.User;
import io.github.acarolinebcosta.docfy.auth.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(AuthenticationException::new);

        if (!user.isActive()) {
            throw new AuthenticationException();
        }

        if (!passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        )) {
            throw new AuthenticationException();
        }

        return user;
    }
}