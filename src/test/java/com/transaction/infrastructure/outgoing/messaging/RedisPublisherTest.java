package com.transaction.infrastructure.outgoing.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.outgoing.messaging.mapper.TransactionMessageMapper;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
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
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisPublisherTest {

    private ReactiveStreamCommands<String, String, String> streamCommands;
    private TransactionMessageMapper mapper;
    private ObjectMapper objectMapper;
    private RedisPublisher redisPublisher;

    private static final String TRANSACTION_CREATED_STREAM = "transaction:created";
    private static final String TRANSACTION_UPDATED_STREAM = "transaction:updated";

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
        Message<TransactionCreatedData> message = createMessage(transaction);
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
        assertEquals("TransactionCreated", capturedData.get("eventType"));
        assertEquals(serializedMessage, capturedData.get("payload"));
    }

    @Test
    void testPublishTransactionUpdatedEventSuccess() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionUpdatedEvent event = new TransactionUpdatedEvent(transaction);
        Message<TransactionCreatedData> message = createMessage(transaction);
        String serializedMessage = "serialized-message";

        // The updated event is converted to created event for reuse of message structure
        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        when(mapper.toTransactionCreated(eventCaptor.capture())).thenReturn(message);
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
        TransactionCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(transaction, capturedEvent.getData());

        verify(objectMapper).writeValueAsString(message);
        
        // Verify stream data structure for updated event
        ArgumentCaptor<Map<String, String>> streamDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamCommands).xadd(eq(TRANSACTION_UPDATED_STREAM), streamDataCaptor.capture());
        
        Map<String, String> capturedData = streamDataCaptor.getValue();
        assertEquals("TransactionUpdated", capturedData.get("eventType"));
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
        Message<TransactionCreatedData> message = createMessage(transaction);
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
        Message<TransactionCreatedData> message = createMessage(transaction);
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
        return new Transaction(
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
                Collections.emptyList()
        );
    }

    private Message<TransactionCreatedData> createMessage(Transaction transaction) {
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
