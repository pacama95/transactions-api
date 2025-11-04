package com.transaction.infrastructure.testutil;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.junit5.api.DBRider;
import com.transaction.infrastructure.testcontainers.PostgresTestResource;
import com.transaction.infrastructure.testcontainers.RedisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all integration tests.
 * Provides common setup, test resources, and utility methods.
 *
 * Features:
 * - PostgreSQL testcontainer with Liquibase migrations
 * - Redis testcontainer for event streaming
 * - DbRider for database state management
 * - REST-Assured configuration
 * - Common test utilities and helpers
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(RedisTestResource.class)
@DBRider
@DBUnit(caseInsensitiveStrategy = Orthography.LOWERCASE)
public abstract class BaseIntegrationTest {

    @Inject
    protected RedisStreamVerifier redisStreamVerifier;

    @BeforeEach
    public void setUp() {
        configureRestAssured();
        cleanupRedisStreams();
    }

    /**
     * Configure REST-Assured for HTTP testing
     */
    private void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = getRestAssuredPort();
        RestAssured.basePath = "/api/transactions";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    /**
     * Get the port for REST-Assured.
     * Override this method if needed for custom port configuration.
     */
    protected int getRestAssuredPort() {
        String port = System.getProperty("quarkus.http.test-port");
        return port != null ? Integer.parseInt(port) : 8081;
    }

    /**
     * Clean up Redis streams between tests to ensure test isolation
     */
    private void cleanupRedisStreams() {
        if (redisStreamVerifier != null) {
            redisStreamVerifier.clearAllStreams();
        }
    }

    /**
     * Helper method to create REST-Assured request specification with JSON content type
     */
    protected io.restassured.specification.RequestSpecification givenJsonRequest() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * Helper method to create REST-Assured request specification for SSE
     */
    protected io.restassured.specification.RequestSpecification givenSseRequest() {
        return RestAssured.given()
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive");
    }

    /**
     * Wait for a specific amount of time (for async operations)
     */
    protected void waitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting", e);
        }
    }
}
