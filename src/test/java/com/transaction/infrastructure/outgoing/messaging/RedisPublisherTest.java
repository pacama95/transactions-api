package com.transaction.infrastructure.outgoing.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.outgoing.messaging.mapper.TransactionMessageMapper;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionDeletedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionUpdatedData;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisPublisherTest {

    private ReactiveStreamCommands<String, String, String> streamCommands;
    private TransactionMessageMapper mapper;
    private ObjectMapper objectMapper;
    private RedisPublisher redisPublisher;

    private static final String TRANSACTION_CREATED_STREAM = "transaction:created";
    private static final String TRANSACTION_UPDATED_STREAM = "transaction:updated";
    private static final String TRANSACTION_DELETED_STREAM = "transaction:deleted";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReactiveRedisDataSource redisDataSource = mock(ReactiveRedisDataSource.class);
        streamCommands = (ReactiveStreamCommands<String, String, String>) mock(ReactiveStreamCommands.class);
        mapper = mock(TransactionMessageMapper.class);
        objectMapper = mock(ObjectMapper.class);

        when(redisDataSource.stream(String.class, String.class, String.class)).thenReturn(streamCommands);

        redisPublisher = new RedisPublisher(redisDataSource, mapper, objectMapper);
    }

    @Test
    void testPublishTransactionCreatedEventSuccess() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionCreatedEvent event = new TransactionCreatedEvent(transaction);
        Message<TransactionCreatedData> message = createMessageTransactionCreated(transaction);
        String serializedMessage = "serialized-message";

        when(mapper.toTransactionCreated(event)).thenReturn(message);
        when(objectMapper.writeValueAsString(message)).thenReturn(serializedMessage);
        doReturn(Uni.createFrom().item("stream-message-id"))
                .when(streamCommands).xadd(eq(TRANSACTION_CREATED_STREAM), any(Map.class));

        // When
        Uni<Void> result = redisPublisher.publish(event);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        verify(mapper).toTransactionCreated(event);
        verify(objectMapper).writeValueAsString(message);

        // Verify stream data structure
        ArgumentCaptor<Map<String, String>> streamDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamCommands).xadd(eq(TRANSACTION_CREATED_STREAM), streamDataCaptor.capture());

        Map<String, String> capturedData = streamDataCaptor.getValue();
        assertEquals(serializedMessage, capturedData.get("payload"));
    }

    @Test
    void testPublishTransactionUpdatedEventSuccess() throws JsonProcessingException {
        // Given
        Transaction previousTransaction = createTransaction();
        Transaction newTransaction = createTransaction();
        com.transaction.domain.model.TransactionUpdateData updateData = 
            new com.transaction.domain.model.TransactionUpdateData(previousTransaction, newTransaction);
        TransactionUpdatedEvent event = new TransactionUpdatedEvent(updateData);
        Message<TransactionUpdatedData> message = createMessageForTransactionUpdated(previousTransaction, newTransaction);
        String serializedMessage = "serialized-message";

        ArgumentCaptor<TransactionUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionUpdatedEvent.class);
        when(mapper.toTransactionUpdated(eventCaptor.capture())).thenReturn(message);
        when(objectMapper.writeValueAsString(message)).thenReturn(serializedMessage);
        doReturn(Uni.createFrom().item("stream-message-id"))
                .when(streamCommands).xadd(eq(TRANSACTION_UPDATED_STREAM), any(Map.class));

        // When
        Uni<Void> result = redisPublisher.publish(event);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        // Verify the updated event contains the update data
        TransactionUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(updateData, capturedEvent.getData());

        verify(objectMapper).writeValueAsString(message);

        // Verify stream data structure for updated event
        ArgumentCaptor<Map<String, String>> streamDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamCommands).xadd(eq(TRANSACTION_UPDATED_STREAM), streamDataCaptor.capture());

        Map<String, String> capturedData = streamDataCaptor.getValue();
        assertEquals(serializedMessage, capturedData.get("payload"));
    }

    @Test
    void testPublishTransactionDeletedEventSuccess() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionDeletedEvent event = new TransactionDeletedEvent(transaction);
        Message<TransactionDeletedData> message = createMessageForTransactionDeleted(transaction);
        String serializedMessage = "serialized-message";

        ArgumentCaptor<TransactionDeletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionDeletedEvent.class);
        when(mapper.toTransactionDeleted(eventCaptor.capture())).thenReturn(message);
        when(objectMapper.writeValueAsString(message)).thenReturn(serializedMessage);
        doReturn(Uni.createFrom().item("stream-message-id"))
                .when(streamCommands).xadd(eq(TRANSACTION_UPDATED_STREAM), any(Map.class));

        // When
        Uni<Void> result = redisPublisher.publish(event);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        // Verify the updated event was converted to created event
        TransactionDeletedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(transaction, capturedEvent.getData());

        verify(objectMapper).writeValueAsString(message);

        // Verify stream data structure for updated event
        ArgumentCaptor<Map<String, String>> streamDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamCommands).xadd(eq(TRANSACTION_DELETED_STREAM), streamDataCaptor.capture());

        Map<String, String> capturedData = streamDataCaptor.getValue();
        assertNotNull(capturedData.get("payload"));
        assertEquals(serializedMessage, capturedData.get("payload"));
    }

    @Test
    void testPublishUnsupportedEventType() {
        // Given
        DomainEvent<Transaction> unsupportedEvent = new UnsupportedDomainEvent(createTransaction());

        // When
        Uni<Void> result = redisPublisher.publish(unsupportedEvent);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceException.class);

        verifyNoInteractions(mapper, objectMapper, streamCommands);
    }

    @Test
    void testPublishJsonSerializationFailure() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionCreatedEvent event = new TransactionCreatedEvent(transaction);
        Message<TransactionCreatedData> message = createMessageTransactionCreated(transaction);
        JsonProcessingException jsonException = new JsonProcessingException("Serialization failed") {
        };

        when(mapper.toTransactionCreated(event)).thenReturn(message);
        when(objectMapper.writeValueAsString(message)).thenThrow(jsonException);

        // When
        Uni<Void> result = redisPublisher.publish(event);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceException.class);

        verify(mapper).toTransactionCreated(event);
        verify(objectMapper).writeValueAsString(message);
        verifyNoInteractions(streamCommands);
    }

    @Test
    void testPublishRedisPublishFailure() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionCreatedEvent event = new TransactionCreatedEvent(transaction);
        Message<TransactionCreatedData> message = createMessageTransactionCreated(transaction);
        String serializedMessage = "serialized-message";
        RuntimeException redisException = new RuntimeException("Redis connection failed");

        when(mapper.toTransactionCreated(event)).thenReturn(message);
        when(objectMapper.writeValueAsString(message)).thenReturn(serializedMessage);
        doReturn(Uni.createFrom().failure(redisException))
                .when(streamCommands).xadd(eq(TRANSACTION_CREATED_STREAM), any(Map.class));

        // When
        Uni<Void> result = redisPublisher.publish(event);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceException.class);

        verify(mapper).toTransactionCreated(event);
        verify(objectMapper).writeValueAsString(message);
        verify(streamCommands).xadd(eq(TRANSACTION_CREATED_STREAM), any(Map.class));
    }

    private Transaction createTransaction() {
        return Transaction.create(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(150.00),
                BigDecimal.valueOf(1.99),
                Currency.USD,
                LocalDate.now(),
                "Test transaction",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }

    private Message<TransactionCreatedData> createMessageTransactionCreated(Transaction transaction) {
        TransactionCreatedData payload = new TransactionCreatedData(
                transaction.getId(),
                transaction.getTicker(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFees(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getIsFractional(),
                transaction.getFractionalMultiplier(),
                transaction.getCommissionCurrency()
        );

        return new Message<>(
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "TransactionCreated",
                payload
        );
    }

    private Message<TransactionUpdatedData> createMessageForTransactionUpdated(Transaction previousTransaction, Transaction newTransaction) {
        TransactionUpdatedData.TransactionSnapshot previousSnapshot = new TransactionUpdatedData.TransactionSnapshot(
                previousTransaction.getId(),
                previousTransaction.getTicker(),
                previousTransaction.getTransactionType(),
                previousTransaction.getQuantity(),
                previousTransaction.getPrice(),
                previousTransaction.getFees(),
                previousTransaction.getCurrency(),
                previousTransaction.getTransactionDate(),
                previousTransaction.getNotes(),
                previousTransaction.getIsFractional(),
                previousTransaction.getFractionalMultiplier(),
                previousTransaction.getCommissionCurrency(),
                previousTransaction.getExchange(),
                previousTransaction.getCountry()
        );

        TransactionUpdatedData.TransactionSnapshot newSnapshot = new TransactionUpdatedData.TransactionSnapshot(
                newTransaction.getId(),
                newTransaction.getTicker(),
                newTransaction.getTransactionType(),
                newTransaction.getQuantity(),
                newTransaction.getPrice(),
                newTransaction.getFees(),
                newTransaction.getCurrency(),
                newTransaction.getTransactionDate(),
                newTransaction.getNotes(),
                newTransaction.getIsFractional(),
                newTransaction.getFractionalMultiplier(),
                newTransaction.getCommissionCurrency(),
                newTransaction.getExchange(),
                newTransaction.getCountry()
        );

        TransactionUpdatedData payload = new TransactionUpdatedData(previousSnapshot, newSnapshot);

        return new Message<>(
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "TransactionUpdated",
                payload
        );
    }

    private Message<TransactionDeletedData> createMessageForTransactionDeleted(Transaction transaction) {
        TransactionDeletedData payload = new TransactionDeletedData(
                transaction.getId(),
                transaction.getTicker(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFees(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getIsFractional(),
                transaction.getFractionalMultiplier(),
                transaction.getCommissionCurrency()
        );

        return new Message<>(
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                "TransactionDeleted",
                payload
        );
    }

    // Test helper for unsupported event type
    private static class UnsupportedDomainEvent extends DomainEvent<Transaction> {
        public UnsupportedDomainEvent(Transaction transaction) {
            super(transaction);
        }
    }
}
