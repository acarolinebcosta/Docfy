package io.github.acarolinebcosta.docfy.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public interface PostgresTestContainer {

    @Container
    @ServiceConnection
    PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("docfy_test")
                    .withUsername("docfy_test")
                    .withPassword("docfy_test");
}