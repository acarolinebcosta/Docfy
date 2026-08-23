package io.github.acarolinebcosta.docfy.auth.domain;

import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest implements PostgresTestContainer {

    private static final String RAW_PASSWORD = "StrongPassword123!";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndRetrieveUserWithEncodedPassword() {
        String encodedPassword =
                passwordEncoder.encode(RAW_PASSWORD);

        User user = new User(
                "admin@docfy.local",
                encodedPassword,
                Role.ADMIN
        );

        userRepository.saveAndFlush(user);

        entityManager.clear();

        Optional<User> storedUser =
                userRepository.findByEmail("admin@docfy.local");

        assertTrue(storedUser.isPresent());

        User persistedUser = storedUser.get();

        assertEquals(
                "admin@docfy.local",
                persistedUser.getEmail()
        );

        assertEquals(
                Role.ADMIN,
                persistedUser.getRole()
        );

        assertTrue(persistedUser.isActive());

        assertNotEquals(
                RAW_PASSWORD,
                persistedUser.getPasswordHash()
        );

        assertTrue(
                passwordEncoder.matches(
                        RAW_PASSWORD,
                        persistedUser.getPasswordHash()
                )
        );
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> user =
                userRepository.findByEmail(
                        "missing@docfy.local"
                );

        assertFalse(user.isPresent());
    }
}