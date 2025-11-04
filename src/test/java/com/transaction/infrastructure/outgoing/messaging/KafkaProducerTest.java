package com.transaction.infrastructure.outgoing.messaging;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("KafkaProducer Tests")
class KafkaProducerTest {

    private KafkaProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        kafkaProducer = new KafkaProducer();
    }

    @Test
    @DisplayName("Should return void successfully when publishing event")
    void testPublishReturnsVoid() {
        // Given
        Transaction transaction = createTransaction();
        DomainEvent<Transaction> event = new TransactionCreatedEvent(transaction);

        // When
        Uni<Void> result = kafkaProducer.publish(event);

        // Then
        Void voidResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        // Verify that the result completes without error
        // The current implementation is a stub that returns void
        assertNotNull(result);
    }

    // ============ HELPER METHODS ============

    private Transaction createTransaction() {
        return Transaction.create(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.now(),
                null,
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }
}
