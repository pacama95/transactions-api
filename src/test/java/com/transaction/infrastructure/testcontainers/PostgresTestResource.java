package com.transaction.infrastructure.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

/**
 * Quarkus test resource that manages a PostgreSQL test container lifecycle.
 * This resource starts a PostgreSQL container before tests run and stops it after tests complete.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";
    private static final String DATABASE_NAME = "transactions_test_db";
    private static final String USERNAME = "test_user";
    private static final String PASSWORD = "test_password";

    private PostgreSQLContainer<?> postgresContainer;

    @Override
    public Map<String, String> start() {
        // Create and start PostgreSQL container
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName(DATABASE_NAME)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withReuse(false); // Don't reuse containers between test runs

        postgresContainer.start();

        // Return configuration properties for Quarkus to use
        Map<String, String> config = new HashMap<>();

        // Reactive datasource configuration
        config.put("quarkus.datasource.reactive.url", postgresContainer.getJdbcUrl()
                .replace("jdbc:", "")
                .replace("postgresql", "postgresql"));
        config.put("quarkus.datasource.username", postgresContainer.getUsername());
        config.put("quarkus.datasource.password", postgresContainer.getPassword());
        config.put("quarkus.datasource.db-kind", "postgresql");

        // JDBC datasource for Liquibase migrations
        config.put("quarkus.datasource.jdbc.url", postgresContainer.getJdbcUrl());

        // Liquibase configuration
        config.put("quarkus.liquibase.migrate-at-start", "true");
        config.put("quarkus.liquibase.clean-at-start", "false");

        // Hibernate configuration for tests
        config.put("quarkus.hibernate-orm.database.generation", "none");
        config.put("quarkus.hibernate-orm.log.sql", "false");

        System.out.println("PostgreSQL Test Container started: " + postgresContainer.getJdbcUrl());

        return config;
    }

    @Override
    public void stop() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
            System.out.println("PostgreSQL Test Container stopped");
        }
    }

    /**
     * Get the running PostgreSQL container instance.
     * Can be used by tests to interact with the container directly if needed.
     */
    public PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }
}
