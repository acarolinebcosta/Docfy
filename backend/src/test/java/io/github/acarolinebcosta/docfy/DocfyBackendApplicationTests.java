package io.github.acarolinebcosta.docfy;

import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocfyBackendApplicationTests implements PostgresTestContainer {

    @Test
    void contextLoads() {
    }
}