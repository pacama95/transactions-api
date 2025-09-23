package com.transaction.infrastructure.outgoing.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.port.output.EventPublisher;
import com.transaction.infrastructure.outgoing.messaging.mapper.TransactionMessageMapper;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.Map;

/**
 * Redis implementation of EventPublisher output port.
 * This adapter publishes domain events to Redis Streams for event sourcing.
 */
@ApplicationScoped
@Named("redisPublisher")
public class RedisPublisher implements EventPublisher<DomainEvent<Transaction>> {

    private final ReactiveStreamCommands<String, String, String> streamCommands;
    private final TransactionMessageMapper mapper;
    private final ObjectMapper objectMapper;

    // Redis stream names
    private static final String TRANSACTION_CREATED_STREAM = "transaction:created";
    private static final String TRANSACTION_UPDATED_STREAM = "transaction:updated";

    public RedisPublisher(ReactiveRedisDataSource redisDataSource,
                          TransactionMessageMapper mapper,
                          ObjectMapper objectMapper) {
        this.streamCommands = redisDataSource.stream(String.class, String.class, String.class);
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Uni<Void> publish(DomainEvent<Transaction> domainEvent) {
        return switch (domainEvent) {
            case TransactionCreatedEvent event -> publishTransactionCreated(event);
            case TransactionUpdatedEvent event -> publishTransactionUpdated(event);
            default -> Uni.createFrom().failure(
                    new ServiceException(
                            Errors.PublishTransactionsErrors.PUBLISH_ERROR,
                            new IllegalArgumentException("Unsupported event type: " + domainEvent.getClass().getSimpleName())
                    )
            );
        };
    }

    /**
     * Publishes a TransactionCreatedEvent to the transaction:created Redis stream.
     */
    private Uni<Void> publishTransactionCreated(TransactionCreatedEvent transactionCreatedEvent) {
        Message<TransactionCreatedData> message = mapper.toTransactionCreated(transactionCreatedEvent);

        return serializeMessage(message)
                .flatMap(serializedMessage -> {
                    // Create stream entry with event metadata and payload
                    Map<String, String> streamData = Map.of(
                            "eventId", message.eventId().toString(),
                            "eventType", "TransactionCreated",
                            "occurredAt", message.occurredAt().toString(),
                            "messageCreatedAt", message.messageCreatedAt().toString(),
                            "payload", serializedMessage
                    );

                    return streamCommands.xadd(TRANSACTION_CREATED_STREAM, streamData)
                            .onItem().invoke(messageId ->
                                    Log.info("Published transaction created event with ID %s and eventId %s to Redis stream %s with messageId %s"
                                            .formatted(
                                                    message.payload().id(),
                                                    message.eventId(),
                                                    TRANSACTION_CREATED_STREAM,
                                                    messageId
                                            )))
                            .replaceWithVoid();
                })
                .onFailure().invoke(throwable ->
                        Log.error("Failed to publish transaction created event with ID %s and eventId %s to Redis stream"
                                .formatted(message.payload().id(), message.eventId()), throwable))
                .onFailure().transform(throwable ->
                        new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable));
    }

    /**
     * Publishes a TransactionUpdatedEvent to the transaction:updated Redis stream.
     */
    private Uni<Void> publishTransactionUpdated(TransactionUpdatedEvent transactionUpdatedEvent) {
        // For now, reuse the same message structure for updated events
        // In the future, this could be mapped to a different message type
        Message<TransactionCreatedData> message = mapper.toTransactionCreated(
                new TransactionCreatedEvent(transactionUpdatedEvent.getData())
        );

        return serializeMessage(message)
                .flatMap(serializedMessage -> {
                    // Create stream entry with event metadata and payload
                    Map<String, String> streamData = Map.of(
                            "eventId", message.eventId().toString(),
                            "eventType", "TransactionUpdated",
                            "occurredAt", message.occurredAt().toString(),
                            "messageCreatedAt", message.messageCreatedAt().toString(),
                            "payload", serializedMessage
                    );

                    return streamCommands.xadd(TRANSACTION_UPDATED_STREAM, streamData)
                            .onItem().invoke(messageId ->
                                    Log.info("Published transaction updated event with ID %s and eventId %s to Redis stream %s with messageId %s"
                                            .formatted(
                                                    message.payload().id(),
                                                    message.eventId(),
                                                    TRANSACTION_UPDATED_STREAM,
                                                    messageId
                                            )))
                            .replaceWithVoid();
                })
                .onFailure().invoke(throwable ->
                        Log.error("Failed to publish transaction updated event with ID %s and eventId %s to Redis stream"
                                .formatted(message.payload().id(), message.eventId()), throwable))
                .onFailure().transform(throwable ->
                        new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable));
    }

    /**
     * Serializes a message to JSON string for Redis stream publishing.
     */
    private Uni<String> serializeMessage(Message<TransactionCreatedData> message) {
        return Uni.createFrom().item(() -> {
            try {
                return objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                throw new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR,
                        new RuntimeException("Failed to serialize message to JSON", e));
            }
        });
    }
}
