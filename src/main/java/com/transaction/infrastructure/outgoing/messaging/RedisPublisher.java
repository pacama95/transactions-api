package com.transaction.infrastructure.outgoing.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.port.output.DomainEventPublisher;
import com.transaction.infrastructure.outgoing.messaging.mapper.TransactionMessageMapper;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionDeletedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionUpdatedData;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.Map;

@ApplicationScoped
@Named("redisPublisher")
public class RedisPublisher implements DomainEventPublisher {

    private static final String TRANSACTION_CREATED_STREAM = "transaction:created";
    private static final String TRANSACTION_UPDATED_STREAM = "transaction:updated";
    private static final String TRANSACTION_DELETED_STREAM = "transaction:deleted";

    private final ReactiveStreamCommands<String, String, String> streamCommands;
    private final TransactionMessageMapper mapper;
    private final ObjectMapper objectMapper;

    public RedisPublisher(ReactiveRedisDataSource redisDataSource,
                          TransactionMessageMapper mapper,
                          ObjectMapper objectMapper) {
        this.streamCommands = redisDataSource.stream(String.class, String.class, String.class);
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Uni<Void> publish(DomainEvent<?> domainEvent) {
        return switch (domainEvent) {
            case TransactionCreatedEvent event -> publishTransactionCreated(event);
            case TransactionUpdatedEvent event -> publishTransactionUpdated(event);
            case TransactionDeletedEvent event -> publishTransactionDeleted(event);
            default -> Uni.createFrom().failure(
                    new ServiceException(
                            Errors.PublishTransactionsErrors.PUBLISH_ERROR,
                            new IllegalArgumentException("Unsupported event type: " + domainEvent.getClass().getSimpleName())
                    )
            );
        };
    }

    private Uni<Void> publishTransactionCreated(TransactionCreatedEvent transactionCreatedEvent) {
        Message<TransactionCreatedData> message = mapper.toTransactionCreated(transactionCreatedEvent);

        return serializeMessage(message)
                .flatMap(serializedMessage -> {
                    Map<String, String> streamData = Map.of("payload", serializedMessage);

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

    private Uni<Void> publishTransactionUpdated(TransactionUpdatedEvent transactionUpdatedEvent) {
        Message<TransactionUpdatedData> message = mapper.toTransactionUpdated(transactionUpdatedEvent);

        return serializeMessage(message)
                .flatMap(serializedMessage -> {
                    Map<String, String> streamData = Map.of("payload", serializedMessage);

                    return streamCommands.xadd(TRANSACTION_UPDATED_STREAM, streamData)
                            .onItem().invoke(messageId ->
                                    Log.info("Published transaction updated event with ID %s and eventId %s to Redis stream %s with messageId %s"
                                            .formatted(
                                                    message.payload().newTransaction().id(),
                                                    message.eventId(),
                                                    TRANSACTION_UPDATED_STREAM,
                                                    messageId
                                            )))
                            .replaceWithVoid();
                })
                .onFailure().invoke(throwable ->
                        Log.error("Failed to publish transaction updated event with ID %s and eventId %s to Redis stream"
                                .formatted(message.payload().newTransaction().id(), message.eventId()), throwable))
                .onFailure().transform(throwable ->
                        new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable));
    }

    private Uni<Void> publishTransactionDeleted(TransactionDeletedEvent transactionDeletedEvent) {
        Message<TransactionDeletedData> message = mapper.toTransactionDeleted(transactionDeletedEvent);

        return serializeMessage(message)
                .flatMap(serializedMessage -> {
                    Map<String, String> streamData = Map.of("payload", serializedMessage);

                    return streamCommands.xadd(TRANSACTION_DELETED_STREAM, streamData)
                            .onItem().invoke(messageId ->
                                    Log.info("Published transaction deleted event with ID %s and eventId %s to Redis stream %s with messageId %s"
                                            .formatted(
                                                    message.payload().id(),
                                                    message.eventId(),
                                                    TRANSACTION_DELETED_STREAM,
                                                    messageId
                                            )))
                            .replaceWithVoid();
                })
                .onFailure().invoke(throwable ->
                        Log.error("Failed to publish transaction deleted event with ID %s and eventId %s to Redis stream"
                                .formatted(message.payload().id(), message.eventId()), throwable))
                .onFailure().transform(throwable ->
                        new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable));
    }

    /**
     * Serializes a message to JSON string for Redis stream publishing.
     */
    private Uni<String> serializeMessage(Message<?> message) {
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
