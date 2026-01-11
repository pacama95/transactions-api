package com.transaction.domain.model;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    // ==================== Creation Tests ====================

    @Test
    void testCreateTransactionWithAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        String ticker = "AAPL";
        TransactionType type = TransactionType.BUY;
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150.50");
        BigDecimal fees = new BigDecimal("9.99");
        Currency currency = Currency.USD;
        LocalDate date = LocalDate.of(2024, 1, 15);
        String notes = "Test transaction";
        Boolean isActive = true;
        Boolean isFractional = false;
        BigDecimal fractionalMultiplier = BigDecimal.ONE;
        Currency commissionCurrency = Currency.USD;
        String exchange = "NYSE";
        String country = "USA";
        String companyName = "Apple Inc.";

        // When
        Transaction transaction = Transaction.create(
                id, ticker, type, quantity, price, fees, currency, date, notes,
                isActive, isFractional, fractionalMultiplier, commissionCurrency,
                exchange, country, companyName
        );

        // Then
        assertNotNull(transaction);
        assertEquals(id, transaction.getId());
        assertEquals(ticker, transaction.getTicker());
        assertEquals(type, transaction.getTransactionType());
        assertEquals(quantity, transaction.getQuantity());
        assertEquals(price, transaction.getPrice());
        assertEquals(fees, transaction.getFees());
        assertEquals(currency, transaction.getCurrency());
        assertEquals(date, transaction.getTransactionDate());
        assertEquals(notes, transaction.getNotes());
        assertEquals(isActive, transaction.getIsActive());
        assertEquals(isFractional, transaction.getIsFractional());
        assertEquals(fractionalMultiplier, transaction.getFractionalMultiplier());
        assertEquals(commissionCurrency, transaction.getCommissionCurrency());
        assertEquals(exchange, transaction.getExchange());
        assertEquals(country, transaction.getCountry());
        assertEquals(companyName, transaction.getCompanyName());
    }

    @Test
    void testCreateTransactionGeneratesCreatedEvent() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = createBasicTransaction(id);

        // Then
        List<DomainEvent<?>> events = transaction.getDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(TransactionCreatedEvent.class, events.get(0));
        TransactionCreatedEvent event = (TransactionCreatedEvent) events.get(0);
        assertEquals(transaction, event.getData());
    }

    @Test
    void testCreateTransactionWithNullOptionalFields() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "AAPL", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("150.50"), null, Currency.USD, LocalDate.now(),
                null, true, false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertNotNull(transaction);
        assertNull(transaction.getFees());
        assertNull(transaction.getNotes());
        assertNull(transaction.getCommissionCurrency());
    }

    @ParameterizedTest(name = "Transaction type: {0}")
    @MethodSource("provideTransactionTypes")
    void testCreateTransactionWithDifferentTypes(TransactionType type) {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "AAPL", type, new BigDecimal("10"), new BigDecimal("150.50"),
                BigDecimal.ZERO, Currency.USD, LocalDate.now(), null, true,
                false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertEquals(type, transaction.getTransactionType());
    }

    // ==================== Update Tests ====================

    @Test
    void testUpdateTransactionAllFields() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        String newTicker = "MSFT";
        TransactionType newType = TransactionType.SELL;
        BigDecimal newQuantity = new BigDecimal("20");
        BigDecimal newPrice = new BigDecimal("420.75");
        BigDecimal newFees = new BigDecimal("15.00");
        Currency newCurrency = Currency.EUR;
        LocalDate newDate = LocalDate.of(2024, 2, 20);
        String newNotes = "Updated notes";
        Boolean newIsFractional = true;
        BigDecimal newFractionalMultiplier = new BigDecimal("0.5");
        Currency newCommissionCurrency = Currency.GBP;
        String newExchange = "NASDAQ";
        String newCountry = "UK";
        String newCompanyName = "Microsoft Corporation";

        // When
        transaction.update(
                newTicker, newType, newQuantity, newPrice, newFees, newCurrency,
                newDate, newNotes, newIsFractional, newFractionalMultiplier,
                newCommissionCurrency, newExchange, newCountry, newCompanyName
        );

        // Then
        assertEquals(newTicker, transaction.getTicker());
        assertEquals(newType, transaction.getTransactionType());
        assertEquals(newQuantity, transaction.getQuantity());
        assertEquals(newPrice, transaction.getPrice());
        assertEquals(newFees, transaction.getFees());
        assertEquals(newCurrency, transaction.getCurrency());
        assertEquals(newDate, transaction.getTransactionDate());
        assertEquals(newNotes, transaction.getNotes());
        assertEquals(newIsFractional, transaction.getIsFractional());
        assertEquals(newFractionalMultiplier, transaction.getFractionalMultiplier());
        assertEquals(newCommissionCurrency, transaction.getCommissionCurrency());
        assertEquals(newExchange, transaction.getExchange());
        assertEquals(newCountry, transaction.getCountry());
        assertEquals(newCompanyName, transaction.getCompanyName());
    }

    @Test
    void testUpdateTransactionGeneratesUpdatedEvent() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        transaction.popEvents(); // Clear creation event

        // When
        transaction.update(
                "MSFT", TransactionType.SELL, new BigDecimal("5"),
                new BigDecimal("420.75"), new BigDecimal("12.50"), Currency.EUR,
                LocalDate.of(2024, 2, 20), "Updated", true, new BigDecimal("0.5"),
                Currency.GBP, "NASDAQ", "USA", "Microsoft Corporation"
        );

        // Then
        List<DomainEvent<?>> events = transaction.getDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(TransactionUpdatedEvent.class, events.get(0));
    }

    @Test
    void testUpdateWithNullValuesKeepsOriginal() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        String originalTicker = transaction.getTicker();
        TransactionType originalType = transaction.getTransactionType();
        BigDecimal originalQuantity = transaction.getQuantity();
        String originalCompanyName = transaction.getCompanyName();

        // When - Update with all null values
        transaction.update(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        // Then - All original values should be preserved
        assertEquals(originalTicker, transaction.getTicker());
        assertEquals(originalType, transaction.getTransactionType());
        assertEquals(originalQuantity, transaction.getQuantity());
        assertEquals(originalCompanyName, transaction.getCompanyName());
    }

    @Test
    void testPartialUpdateOnlyChangesSpecifiedFields() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        String originalTicker = transaction.getTicker();
        BigDecimal originalQuantity = transaction.getQuantity();
        String newCompanyName = "New Company Name Inc.";

        // When - Only update companyName
        transaction.update(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, newCompanyName
        );

        // Then
        assertEquals(originalTicker, transaction.getTicker());
        assertEquals(originalQuantity, transaction.getQuantity());
        assertEquals(newCompanyName, transaction.getCompanyName());
    }

    // ==================== Calculation Tests ====================

    @Test
    void testGetTotalValue() {
        // Given
        Transaction transaction = Transaction.create(
                UUID.randomUUID(), "AAPL", TransactionType.BUY,
                new BigDecimal("10"), new BigDecimal("150.50"), BigDecimal.ZERO,
                Currency.USD, LocalDate.now(), null, true, false, BigDecimal.ONE,
                null, "NYSE", "USA", "Apple Inc."
        );

        // When
        BigDecimal totalValue = transaction.getTotalValue();

        // Then
        assertEquals(new BigDecimal("1505.00"), totalValue);
    }

    @Test
    void testGetTotalCostWithFees() {
        // Given
        Transaction transaction = Transaction.create(
                UUID.randomUUID(), "AAPL", TransactionType.BUY,
                new BigDecimal("10"), new BigDecimal("150.50"),
                new BigDecimal("9.99"), Currency.USD, LocalDate.now(), null,
                true, false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
        );

        // When
        BigDecimal totalCost = transaction.getTotalCost();

        // Then
        assertEquals(new BigDecimal("1514.99"), totalCost);
    }

    @Test
    void testGetTotalCostWithoutFees() {
        // Given
        Transaction transaction = Transaction.create(
                UUID.randomUUID(), "AAPL", TransactionType.BUY,
                new BigDecimal("10"), new BigDecimal("150.50"), null,
                Currency.USD, LocalDate.now(), null, true, false, BigDecimal.ONE,
                null, "NYSE", "USA", "Apple Inc."
        );

        // When
        BigDecimal totalCost = transaction.getTotalCost();

        // Then
        assertEquals(new BigDecimal("1505.00"), totalCost);
    }

    @Test
    void testGetTotalCostWithZeroFees() {
        // Given
        Transaction transaction = Transaction.create(
                UUID.randomUUID(), "AAPL", TransactionType.BUY,
                new BigDecimal("10"), new BigDecimal("150.50"), BigDecimal.ZERO,
                Currency.USD, LocalDate.now(), null, true, false, BigDecimal.ONE,
                null, "NYSE", "USA", "Apple Inc."
        );

        // When
        BigDecimal totalCost = transaction.getTotalCost();

        // Then
        assertEquals(new BigDecimal("1505.00"), totalCost);
    }

    // ==================== Domain Events Tests ====================

    @Test
    void testGetDomainEventsReturnsUnmodifiableList() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());

        // When
        List<DomainEvent<?>> events = transaction.getDomainEvents();

        // Then
        assertThrows(UnsupportedOperationException.class, () -> events.clear());
    }

    @Test
    void testPopEventsClearsAndReturnsEvents() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        int initialEventCount = transaction.getDomainEvents().size();

        // When
        List<DomainEvent<?>> poppedEvents = transaction.popEvents();

        // Then
        assertEquals(initialEventCount, poppedEvents.size());
        assertTrue(transaction.getDomainEvents().isEmpty());
    }

    @Test
    void testMultipleUpdatesGenerateMultipleEvents() {
        // Given
        Transaction transaction = createBasicTransaction(UUID.randomUUID());
        transaction.popEvents(); // Clear creation event

        // When
        transaction.update("MSFT", null, null, null, null, null, null, null,
                null, null, null, null, null, "Microsoft Corporation");
        transaction.update("GOOGL", null, null, null, null, null, null, null,
                null, null, null, null, null, "Alphabet Inc.");

        // Then
        List<DomainEvent<?>> events = transaction.getDomainEvents();
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e instanceof TransactionUpdatedEvent));
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorWithEmptyEventsList() {
        // Given
        UUID id = UUID.randomUUID();
        List<DomainEvent<?>> emptyEvents = new ArrayList<>();

        // When
        Transaction transaction = new Transaction(
                id, "AAPL", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), null, true, false, BigDecimal.ONE, null,
                "NYSE", "USA", "Apple Inc.", emptyEvents
        );

        // Then
        assertNotNull(transaction);
        assertTrue(transaction.getDomainEvents().isEmpty());
    }

    @Test
    void testPublicConstructorWithoutId() {
        // Given & When
        Transaction transaction = new Transaction(
                "AAPL", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), null, true, false, BigDecimal.ONE, null,
                "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertNotNull(transaction);
        assertNull(transaction.getId());
        assertTrue(transaction.getDomainEvents().isEmpty());
    }

    // ==================== Edge Cases and Special Scenarios ====================

    @Test
    void testFractionalTransaction() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "AAPL", TransactionType.BUY, new BigDecimal("0.5"),
                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), "Fractional share", true, true,
                new BigDecimal("0.5"), Currency.USD, "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertTrue(transaction.getIsFractional());
        assertEquals(new BigDecimal("0.5"), transaction.getFractionalMultiplier());
    }

    @Test
    void testTransactionWithDifferentCurrencies() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "AAPL", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("150.50"), new BigDecimal("9.99"), Currency.EUR,
                LocalDate.now(), null, true, false, BigDecimal.ONE, Currency.GBP,
                "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertEquals(Currency.EUR, transaction.getCurrency());
        assertEquals(Currency.GBP, transaction.getCommissionCurrency());
    }

    @Test
    void testTransactionWithLongCompanyName() {
        // Given
        String longCompanyName = "Very Long Company Name Corporation International Holdings Limited Inc.";
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "TEST", TransactionType.BUY, new BigDecimal("1"),
                new BigDecimal("100.00"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), null, true, false, BigDecimal.ONE, null,
                "NYSE", "USA", longCompanyName
        );

        // Then
        assertEquals(longCompanyName, transaction.getCompanyName());
    }

    @Test
    void testTransactionWithSpecialCharactersInCompanyName() {
        // Given
        String specialCharsName = "AT&T Inc. - Société Générale & Co., Ltd.";
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "T", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("25.50"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), null, true, false, BigDecimal.ONE, null,
                "NYSE", "USA", specialCharsName
        );

        // Then
        assertEquals(specialCharsName, transaction.getCompanyName());
    }

    @Test
    void testDividendTransaction() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        Transaction transaction = Transaction.create(
                id, "AAPL", TransactionType.DIVIDEND, new BigDecimal("10"),
                new BigDecimal("0.25"), BigDecimal.ZERO, Currency.USD,
                LocalDate.now(), "Quarterly dividend", true, false,
                BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
        );

        // Then
        assertEquals(TransactionType.DIVIDEND, transaction.getTransactionType());
        assertEquals(new BigDecimal("2.50"), transaction.getTotalValue());
    }

    // ==================== Helper Methods ====================

    private Transaction createBasicTransaction(UUID id) {
        return Transaction.create(
                id, "AAPL", TransactionType.BUY, new BigDecimal("10"),
                new BigDecimal("150.50"), new BigDecimal("9.99"), Currency.USD,
                LocalDate.of(2024, 1, 15), "Test transaction", true, false,
                BigDecimal.ONE, Currency.USD, "NYSE", "USA", "Apple Inc."
        );
    }

    static Stream<Arguments> provideTransactionTypes() {
        return Stream.of(
                Arguments.of(TransactionType.BUY),
                Arguments.of(TransactionType.SELL),
                Arguments.of(TransactionType.DIVIDEND)
        );
    }
}
