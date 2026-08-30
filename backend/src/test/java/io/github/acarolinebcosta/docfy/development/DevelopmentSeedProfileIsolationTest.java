package io.github.acarolinebcosta.docfy.development;

import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DevelopmentSeedProfileIsolationTest
        implements PostgresTestContainer {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotLoadDevelopmentSeedOutsideDevProfile() {
        assertTrue(
                applicationContext
                        .getBeansOfType(DevelopmentDataSeeder.class)
                        .isEmpty()
        );
        assertTrue(
                applicationContext
                        .getBeansOfType(DevelopmentSeedService.class)
                        .isEmpty()
        );
    }
}
