package com.transaction.infrastructure.testutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionDeletedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionUpdatedData;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.stream.StreamMessage;
import io.quarkus.redis.datasource.stream.XReadArgs;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class for verifying events published to Redis streams in integration tests.
 * Provides methods to read, verify, and assert on events in Redis streams.
 */
@ApplicationScoped
public class RedisStreamVerifier {

    private static final String TRANSACTION_CREATED_STREAM = "transaction:created";
    private static final String TRANSACTION_UPDATED_STREAM = "transaction:updated";
    private static final String TRANSACTION_DELETED_STREAM = "transaction:deleted";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private final ReactiveStreamCommands<String, String, String> streamCommands;
    private final ObjectMapper objectMapper;

    public RedisStreamVerifier(ReactiveRedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.streamCommands = redisDataSource.stream(String.class, String.class, String.class);
        this.objectMapper = objectMapper;
    }

    /**
     * Wait for and verify that a transaction created event was published
     */
    public Message<TransactionCreatedData> waitForCreatedEvent(UUID transactionId) {
        return waitForCreatedEvent(transactionId, DEFAULT_TIMEOUT);
    }

    /**
     * Wait for and verify that a transaction created event was published with custom timeout
     */
    public Message<TransactionCreatedData> waitForCreatedEvent(UUID transactionId, Duration timeout) {
        Log.info("Waiting for transaction created event for transaction ID: " + transactionId);

        return await()
                .atMost(timeout)
                .pollInterval(POLL_INTERVAL)
                .ignoreExceptions()
                .until(() -> findCreatedEvent(transactionId), Optional::isPresent)
                .orElseThrow(() -> new AssertionError("No created event found for transaction ID: " + transactionId));
    }

    /**
     * Wait for and verify that a transaction updated event was published
     */
    public Message<TransactionUpdatedData> waitForUpdatedEvent(UUID transactionId) {
        return waitForUpdatedEvent(transactionId, DEFAULT_TIMEOUT);
    }

    /**
     * Wait for and verify that a transaction updated event was published with custom timeout
     */
    public Message<TransactionUpdatedData> waitForUpdatedEvent(UUID transactionId, Duration timeout) {
        Log.info("Waiting for transaction updated event for transaction ID: " + transactionId);

        return await()
                .atMost(timeout)
                .pollInterval(POLL_INTERVAL)
                .ignoreExceptions()
                .until(() -> findUpdatedEvent(transactionId), Optional::isPresent)
                .orElseThrow(() -> new AssertionError("No updated event found for transaction ID: " + transactionId));
    }

    /**
     * Wait for and verify that a transaction deleted event was published
     */
    public Message<TransactionDeletedData> waitForDeletedEvent(UUID transactionId) {
        return waitForDeletedEvent(transactionId, DEFAULT_TIMEOUT);
    }

    /**
     * Wait for and verify that a transaction deleted event was published with custom timeout
     */
    public Message<TransactionDeletedData> waitForDeletedEvent(UUID transactionId, Duration timeout) {
        Log.info("Waiting for transaction deleted event for transaction ID: " + transactionId);

        return await()
                .atMost(timeout)
                .pollInterval(POLL_INTERVAL)
                .ignoreExceptions()
                .until(() -> findDeletedEvent(transactionId), Optional::isPresent)
                .orElseThrow(() -> new AssertionError("No deleted event found for transaction ID: " + transactionId));
    }

    /**
     * Assert that an event was published to a specific stream
     */
    public void assertEventPublished(String streamKey, UUID eventId) {
        assertEventPublished(streamKey, eventId, DEFAULT_TIMEOUT);
    }

    /**
     * Assert that an event was published to a specific stream with custom timeout
     */
    public void assertEventPublished(String streamKey, UUID eventId, Duration timeout) {
        await()
                .atMost(timeout)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> {
                    List<StreamMessage<String, String, String>> messages = readAllMessages(streamKey);
                    boolean found = messages.stream()
                            .anyMatch(msg -> containsEventId(msg, eventId));
                    assertTrue(found, "Event with ID " + eventId + " not found in stream " + streamKey);
                });
    }

    /**
     * Assert that an event with specific payload was published
     */
    public void assertEventPayload(String streamKey, UUID eventId, Consumer<String> payloadAssertion) {
        List<StreamMessage<String, String, String>> messages = readAllMessages(streamKey);

        messages.stream()
                .filter(msg -> containsEventId(msg, eventId))
                .findFirst()
                .ifPresentOrElse(
                        msg -> payloadAssertion.accept(msg.payload().get("payload")),
                        () -> fail("Event with ID " + eventId + " not found in stream " + streamKey)
                );
    }

    /**
     * Get all messages from a stream since the beginning
     */
    public List<StreamMessage<String, String, String>> readAllMessages(String streamKey) {
        return streamCommands.xread(streamKey, "0")
                .await()
                .atMost(Duration.ofSeconds(5));
    }

    /**
     * Get the count of messages in a stream
     */
    public long getStreamLength(String streamKey) {
        return streamCommands.xlen(streamKey)
                .await()
                .atMost(Duration.ofSeconds(5));
    }

    /**
     * Clear all transaction streams
     */
    public void clearAllStreams() {
        clearStream(TRANSACTION_CREATED_STREAM);
        clearStream(TRANSACTION_UPDATED_STREAM);
        clearStream(TRANSACTION_DELETED_STREAM);
    }

    /**
     * Clear a specific stream
     */
    public void clearStream(String streamKey) {
        try {
            // Delete the stream key to clear all messages
            streamCommands.getDataSource()
                    .key()
                    .del(streamKey)
                    .await()
                    .atMost(Duration.ofSeconds(5));
            Log.info("Cleared Redis stream: " + streamKey);
        } catch (Exception e) {
            Log.warn("Failed to clear stream " + streamKey + ": " + e.getMessage());
        }
    }

    // Private helper methods

    private Optional<Message<TransactionCreatedData>> findCreatedEvent(UUID transactionId) {
        List<StreamMessage<String, String, String>> messages = readAllMessages(TRANSACTION_CREATED_STREAM);

        return messages.stream()
                .map(msg -> deserializeMessage(msg, new TypeReference<Message<TransactionCreatedData>>() {}))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(msg -> msg.payload().id().equals(transactionId))
                .findFirst();
    }

    private Optional<Message<TransactionUpdatedData>> findUpdatedEvent(UUID transactionId) {
        List<StreamMessage<String, String, String>> messages = readAllMessages(TRANSACTION_UPDATED_STREAM);

        return messages.stream()
                .map(msg -> deserializeMessage(msg, new TypeReference<Message<TransactionUpdatedData>>() {}))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(msg -> msg.payload().newTransaction().id().equals(transactionId))
                .findFirst();
    }

    private Optional<Message<TransactionDeletedData>> findDeletedEvent(UUID transactionId) {
        List<StreamMessage<String, String, String>> messages = readAllMessages(TRANSACTION_DELETED_STREAM);

        return messages.stream()
                .map(msg -> deserializeMessage(msg, new TypeReference<Message<TransactionDeletedData>>() {}))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(msg -> msg.payload().id().equals(transactionId))
                .findFirst();
    }

    private <T> Optional<Message<T>> deserializeMessage(
            StreamMessage<String, String, String> streamMessage,
            TypeReference<Message<T>> typeRef) {
        try {
            String payload = streamMessage.payload().get("payload");
            if (payload == null) {
                return Optional.empty();
            }
            Message<T> message = objectMapper.readValue(payload, typeRef);
            return Optional.of(message);
        } catch (JsonProcessingException e) {
            Log.warn("Failed to deserialize message: " + e.getMessage());
            return Optional.empty();
        }
    }

    private boolean containsEventId(StreamMessage<String, String, String> msg, UUID eventId) {
        try {
            String payload = msg.payload().get("payload");
            if (payload == null) {
                return false;
            }
            // Parse just enough to check eventId
            Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {});
            String messageEventId = (String) map.get("eventId");
            return eventId.toString().equals(messageEventId);
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
