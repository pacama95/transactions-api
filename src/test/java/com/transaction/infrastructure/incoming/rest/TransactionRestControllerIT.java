package com.transaction.infrastructure.incoming.rest;

import com.github.database.rider.core.api.dataset.DataSet;
import com.transaction.infrastructure.incoming.rest.dto.CreateTransactionRequest;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.testutil.BaseIntegrationTest;
import com.transaction.infrastructure.testutil.TransactionTestDataBuilder;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Transaction REST Controller.
 * Tests REST endpoints and verifies Redis event publishing.
 */
class TransactionRestControllerIT extends BaseIntegrationTest {

    @Test
    @DataSet(value = "datasets/empty-db.yml", cleanBefore = true)
    void shouldCreateTransaction_AndPublishToRedis() {
        // Given
        CreateTransactionRequest request = TransactionTestDataBuilder.defaultCreateRequest();

        // When
        String transactionIdStr = given()
                .contentType(ContentType.JSON)
                .body(TransactionTestDataBuilder.toJson(request))
                .when()
                .post("/")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("ticker", equalTo("AAPL"))
                .body("transactionType", equalTo("BUY"))
                .body("quantity", equalTo(100.0f))
                .body("price", equalTo(150.25f))
                .extract()
                .path("id");

        UUID transactionId = UUID.fromString(transactionIdStr);

        // Then - verify Redis event was published
        Message<TransactionCreatedData> event = redisStreamVerifier.waitForCreatedEvent(transactionId);

        assertNotNull(event);
        assertEquals(transactionId, event.payload().id());
        assertEquals("AAPL", event.payload().ticker());
        assertEquals("BUY", event.payload().transactionType());
    }

    @Test
    @DataSet(value = "datasets/transactions.yml", cleanBefore = true)
    void shouldGetTransactionById() {
        // Given
        String existingId = "123e4567-e89b-12d3-a456-426614174000";

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}", existingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(existingId))
                .body("ticker", equalTo("AAPL"))
                .body("transactionType", equalTo("BUY"));
    }

    @Test
    @DataSet(value = "datasets/empty-db.yml", cleanBefore = true)
    void shouldReturn404_WhenTransactionNotFound() {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}", nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    @DataSet(value = "datasets/transactions.yml", cleanBefore = true)
    void shouldGetAllTransactions() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .body("$", hasSize(4));
    }

    @Test
    @DataSet(value = "datasets/transactions-by-ticker.yml", cleanBefore = true)
    void shouldGetTransactionsByTicker() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/ticker/AAPL")
                .then()
                .statusCode(200)
                .body("$", hasSize(4))
                .body("[0].ticker", equalTo("AAPL"))
                .body("[1].ticker", equalTo("AAPL"))
                .body("[2].ticker", equalTo("AAPL"))
                .body("[3].ticker", equalTo("AAPL"));
    }

    @Test
    @DataSet(value = "datasets/transactions.yml", cleanBefore = true)
    void shouldDeleteTransaction_AndPublishToRedis() {
        // Given
        String existingId = "123e4567-e89b-12d3-a456-426614174000";
        UUID transactionId = UUID.fromString(existingId);

        // When
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/{id}", existingId)
                .then()
                .statusCode(204);

        // Then - verify transaction is deleted
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/{id}", existingId)
                .then()
                .statusCode(404);

        // Then - verify Redis event was published
        var deletedEvent = redisStreamVerifier.waitForDeletedEvent(transactionId);
        assertNotNull(deletedEvent);
        assertEquals(transactionId, deletedEvent.payload().id());
    }

    @Test
    @DataSet(value = "datasets/transactions.yml", cleanBefore = true)
    void shouldGetCount() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/count")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    @Test
    @DataSet(value = "datasets/transactions-by-ticker.yml", cleanBefore = true)
    void shouldGetCountByTicker() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/count/AAPL")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }
}
