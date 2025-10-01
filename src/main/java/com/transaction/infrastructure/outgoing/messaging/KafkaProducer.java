package com.transaction.infrastructure.outgoing.messaging;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.port.output.DomainEventPublisher;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Kafka implementation of EventPublisher output port.
 * This adapter publishes domain events to Kafka topics following hexagonal architecture principles.
 */
@ApplicationScoped
@Named("kafkaProducer")
public class KafkaProducer implements DomainEventPublisher {

    //    private final MutinyEmitter<Message<TransactionCreatedData>> transactionEmitter;
//    private final TransactionMessageMapper mapper;
//
//    public KafkaProducer(
//            @Channel("transaction-create") MutinyEmitter<Message<TransactionCreatedData>> transactionEmitter,
//            TransactionMessageMapper mapper) {
//        this.transactionEmitter = transactionEmitter;
//        this.mapper = mapper;
//    }
//
    @Override
    public Uni<Void> publish(DomainEvent<?> domainEvent) {
//        return switch (domainEvent) {
//            case TransactionCreatedEvent event -> publishTransactionCreated(event);
//            case TransactionUpdatedEvent event -> publishTransactionUpdated(event);
//            default -> Uni.createFrom().failure(
//                    new ServiceException(
//                        Errors.PublishTransactionsErrors.PUBLISH_ERROR,
//                        new IllegalArgumentException("Unsupported event type: " + domainEvent.getClass().getSimpleName())
//                    )
//            );
//        };
        return Uni.createFrom().voidItem();
    }
//
//    /**
//     * Publishes a TransactionCreatedEvent to the transaction-create channel.
//     */
//    private Uni<Void> publishTransactionCreated(TransactionCreatedEvent transactionCreatedEvent) {
//        Message<TransactionCreatedData> message = mapper.toTransactionCreated(transactionCreatedEvent);
//
//        return transactionEmitter.send(message)
//                .onFailure().transform(throwable -> {
//                    Log.error("Failed to publish transaction created event with ID %s and eventId %s"
//                            .formatted(message.payload().id(), message.eventId()));
//                    return new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable);
//                });
//    }
//
//    /**
//     * Publishes a TransactionUpdatedEvent to the transaction-create channel.
//     * Note: Using same channel for now, but could be routed to different topic if needed.
//     */
//    private Uni<Void> publishTransactionUpdated(TransactionUpdatedEvent transactionUpdatedEvent) {
//        // For now, reuse the same mapper and channel for updated events
//        // In the future, this could be mapped to a different message type or topic
//        Message<TransactionCreatedData> message = mapper.toTransactionCreated(
//            new TransactionCreatedEvent(transactionUpdatedEvent.getData())
//        );
//
//        return transactionEmitter.send(message)
//                .onFailure().transform(throwable -> {
//                    Log.error("Failed to publish transaction updated event with ID %s and eventId %s"
//                            .formatted(message.payload().id(), message.eventId()));
//                    return new ServiceException(Errors.PublishTransactionsErrors.PUBLISH_ERROR, throwable);
//                });
//    }
}
