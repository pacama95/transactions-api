package com.transaction.infrastructure.outgoing.messaging;

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
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.reactive.messaging.Channel;

/**
 * Kafka implementation of EventPublisher output port.
 * This adapter publishes domain events to Kafka topics following hexagonal architecture principles.
 */
@ApplicationScoped
@Named("kafkaProducer")
public class KafkaProducer implements EventPublisher<DomainEvent<Transaction>> {

    private final MutinyEmitter<Message<TransactionCreatedData>> transactionEmitter;
    private final TransactionMessageMapper mapper;

    public KafkaProducer(
            @Channel("transaction-create") MutinyEmitter<Message<TransactionCreatedData>> transactionEmitter,
            TransactionMessageMapper mapper) {
        this.transactionEmitter = transactionEmitter;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> publish(DomainEvent<Transaction> domainEvent) {
        return switch (domainEvent) {
            case TransactionCreatedEvent event -> publishTransactionCreated(event);
            case TransactionUpdatedEvent event -> publishTransactionUpdated(event);
            default -> Uni.createFrom().failure(
                    new ServiceException(
                        Errors.PublishTransaction.PUBLISH_ERROR,
                        new IllegalArgumentException("Unsupported event type: " + domainEvent.getClass().getSimpleName())
                    )
            );
        };
    }

    /**
     * Publishes a TransactionCreatedEvent to the transaction-create channel.
     */
    private Uni<Void> publishTransactionCreated(TransactionCreatedEvent transactionCreatedEvent) {
        Message<TransactionCreatedData> message = mapper.toTransactionCreated(transactionCreatedEvent);

        return transactionEmitter.send(message)
                .onFailure().transform(throwable -> {
                    Log.error("Failed to publish transaction created event with ID %s and eventId %s"
                            .formatted(message.payload().id(), message.eventId()));
                    return new ServiceException(Errors.PublishTransaction.PUBLISH_ERROR, throwable);
                });
    }

    /**
     * Publishes a TransactionUpdatedEvent to the transaction-create channel.
     * Note: Using same channel for now, but could be routed to different topic if needed.
     */
    private Uni<Void> publishTransactionUpdated(TransactionUpdatedEvent transactionUpdatedEvent) {
        // For now, reuse the same mapper and channel for updated events
        // In the future, this could be mapped to a different message type or topic
        Message<TransactionCreatedData> message = mapper.toTransactionCreated(
            new TransactionCreatedEvent(transactionUpdatedEvent.getData())
        );

        return transactionEmitter.send(message)
                .onFailure().transform(throwable -> {
                    Log.error("Failed to publish transaction updated event with ID %s and eventId %s"
                            .formatted(message.payload().id(), message.eventId()));
                    return new ServiceException(Errors.PublishTransaction.PUBLISH_ERROR, throwable);
                });
    }
}
