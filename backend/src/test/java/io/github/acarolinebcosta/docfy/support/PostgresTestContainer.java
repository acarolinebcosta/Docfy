package io.github.acarolinebcosta.docfy.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

public interface PostgresTestContainer {

    @ServiceConnection
    PostgreSQLContainer POSTGRES = startPostgres();

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres =
                new PostgreSQLContainer("postgres:17-alpine")
                        .withDatabaseName("docfy_test")
                        .withUsername("docfy_test")
                        .withPassword("docfy_test");

        postgres.start();

        return postgres;
    }
}
