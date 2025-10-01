package com.transaction.infrastructure.outgoing.messaging.mapper;

import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionDeletedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionUpdatedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMessageMapperTest {
    private TransactionMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TransactionMessageMapper.class);
    }

    @Test
    void toTransactionCreated_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Transaction tx = Transaction.create(
                id,
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10.000000"),
                new BigDecimal("123.4500"),
                new BigDecimal("1.2500"),
                Currency.USD,
                LocalDate.of(2024, 6, 1),
                "Created notes",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        TransactionCreatedEvent event = new TransactionCreatedEvent(tx);

        Message<TransactionCreatedData> msg = mapper.toTransactionCreated(event);

        assertNotNull(msg);
        assertEquals(event.getEventId(), msg.eventId());
        assertEquals(event.getOccurredAt(), msg.occurredAt());
        assertNotNull(msg.messageCreatedAt());
        assertFalse(msg.messageCreatedAt().isBefore(event.getOccurredAt()));

        TransactionCreatedData p = msg.payload();
        assertNotNull(p);
        assertEquals(id, p.id());
        assertEquals("AAPL", p.ticker());
        assertEquals(TransactionType.BUY, p.transactionType());
        assertEquals(new BigDecimal("10.000000"), p.quantity());
        assertEquals(new BigDecimal("123.4500"), p.price());
        assertEquals(new BigDecimal("1.2500"), p.fees());
        assertEquals(Currency.USD, p.currency());
        assertEquals(LocalDate.of(2024, 6, 1), p.transactionDate());
        assertEquals("Created notes", p.notes());
        assertEquals(false, p.isFractional());
        assertEquals(BigDecimal.ONE, p.fractionalMultiplier());
        assertEquals(Currency.USD, p.commissionCurrency());
    }

    @Test
    void toTransactionUpdated_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Transaction tx = Transaction.create(
                id,
                "MSFT",
                TransactionType.SELL,
                new BigDecimal("5.500000"),
                new BigDecimal("250.7500"),
                new BigDecimal("2.0000"),
                Currency.EUR,
                LocalDate.of(2024, 7, 15),
                "Initial notes",
                true,
                true,
                new BigDecimal("0.5000"),
                Currency.GBP,
                "LSE",
                "UK"
        );

        // apply an update to generate an updated event's data state
        tx.update(
                "MSFT",
                TransactionType.SELL,
                new BigDecimal("6.000000"),
                new BigDecimal("260.0000"),
                new BigDecimal("2.5000"),
                Currency.GBP,
                LocalDate.of(2024, 8, 1),
                "Updated notes",
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        // Get the event that was generated (index 1 because Transaction.create adds a TransactionCreatedEvent first)
        List<com.transaction.domain.event.DomainEvent<?>> events = tx.popEvents();
        TransactionUpdatedEvent event = (TransactionUpdatedEvent) events.stream()
                .filter(e -> e instanceof TransactionUpdatedEvent)
                .findFirst()
                .orElseThrow();

        Message<TransactionUpdatedData> msg = mapper.toTransactionUpdated(event);

        assertNotNull(msg);
        assertEquals(event.getEventId(), msg.eventId());
        assertEquals(event.getOccurredAt(), msg.occurredAt());
        assertNotNull(msg.messageCreatedAt());
        assertFalse(msg.messageCreatedAt().isBefore(event.getOccurredAt()));

        TransactionUpdatedData p = msg.payload();
        assertNotNull(p);

        // Verify previous transaction state
        TransactionUpdatedData.TransactionSnapshot previous = p.previousTransaction();
        assertNotNull(previous);
        assertEquals(id, previous.id());
        assertEquals("MSFT", previous.ticker());
        assertEquals(TransactionType.SELL, previous.transactionType());
        assertEquals(new BigDecimal("5.500000"), previous.quantity());
        assertEquals(new BigDecimal("250.7500"), previous.price());
        assertEquals(new BigDecimal("2.0000"), previous.fees());
        assertEquals(Currency.EUR, previous.currency());
        assertEquals(LocalDate.of(2024, 7, 15), previous.transactionDate());
        assertEquals("Initial notes", previous.notes());
        assertEquals(true, previous.isFractional());
        assertEquals(new BigDecimal("0.5000"), previous.fractionalMultiplier());
        assertEquals(Currency.GBP, previous.commissionCurrency());
        assertEquals("LSE", previous.exchange());
        assertEquals("UK", previous.country());

        // Verify new transaction state
        TransactionUpdatedData.TransactionSnapshot newTx = p.newTransaction();
        assertNotNull(newTx);
        assertEquals(id, newTx.id());
        assertEquals("MSFT", newTx.ticker());
        assertEquals(TransactionType.SELL, newTx.transactionType());
        assertEquals(new BigDecimal("6.000000"), newTx.quantity());
        assertEquals(new BigDecimal("260.0000"), newTx.price());
        assertEquals(new BigDecimal("2.5000"), newTx.fees());
        assertEquals(Currency.GBP, newTx.currency());
        assertEquals(LocalDate.of(2024, 8, 1), newTx.transactionDate());
        assertEquals("Updated notes", newTx.notes());
        assertEquals(false, newTx.isFractional());
        assertEquals(BigDecimal.ONE, newTx.fractionalMultiplier());
        assertEquals(Currency.USD, newTx.commissionCurrency());
        assertEquals("NASDAQ", newTx.exchange());
        assertEquals("USA", newTx.country());
    }

    @Test
    void toTransactionDeleted_mapsIdOnly() {
        UUID id = UUID.randomUUID();
        Transaction tx = Transaction.create(
                id,
                "TSLA",
                TransactionType.BUY,
                new BigDecimal("1.000000"),
                new BigDecimal("100.0000"),
                new BigDecimal("0.0000"),
                Currency.USD,
                LocalDate.of(2024, 1, 1),
                null,
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        TransactionDeletedEvent event = new TransactionDeletedEvent(tx);

        Message<TransactionDeletedData> msg = mapper.toTransactionDeleted(event);

        assertNotNull(msg);
        assertEquals(event.getEventId(), msg.eventId());
        assertEquals(event.getOccurredAt(), msg.occurredAt());
        assertNotNull(msg.messageCreatedAt());
        assertFalse(msg.messageCreatedAt().isBefore(event.getOccurredAt()));

        TransactionDeletedData p = msg.payload();
        assertNotNull(p);
        assertEquals(id, p.id());
    }
}


