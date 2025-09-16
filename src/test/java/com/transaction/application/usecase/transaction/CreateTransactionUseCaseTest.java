package com.transaction.application.usecase.transaction;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.output.EventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for CreateTransactionUseCase.
 * 
 * Tests both transaction persistence and domain event publishing following
 * the user's preferences for explicit mocking and UniAssertSubscriber usage.
 */
class CreateTransactionUseCaseTest {
    private TransactionRepository transactionRepository;
    private EventPublisher<DomainEvent<Transaction>> eventPublisher;
    private CreateTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(EventPublisher.class);
        useCase = new CreateTransactionUseCase();
        useCase.transactionRepository = transactionRepository;
        useCase.eventPublisher = eventPublisher;
    }

    @Test
    void testExecuteSuccess() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();
        
        // Mock repository to return the same transaction but with an ID (simulating save)
        // The key insight: return the input transaction but with an ID assigned
        // This preserves the original domain events that were generated
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);
                    // Repository mock preserves domain events for testing
                    
                    // Create a new transaction that simulates having an ID assigned by the database
                    // while preserving the original domain events
                    Transaction savedTransaction = new Transaction(
                            transactionId, // This is what the "database" would assign
                            inputTransaction.getTicker(),
                            inputTransaction.getTransactionType(),
                            inputTransaction.getQuantity(),
                            inputTransaction.getPrice(),
                            inputTransaction.getFees(),
                            inputTransaction.getCurrency(),
                            inputTransaction.getTransactionDate(),
                            inputTransaction.getNotes(),
                            inputTransaction.getIsActive(),
                            inputTransaction.getIsFractional(),
                            inputTransaction.getFractionalMultiplier(),
                            inputTransaction.getCommissionCurrency(),
                            // Important: Create a new ArrayList to make events mutable for popEvents()
                            new java.util.ArrayList<>(inputTransaction.getDomainEvents())
                    );
                    // Return transaction with preserved mutable events
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        Transaction actualTransaction = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualTransaction);
        assertEquals(transactionId, actualTransaction.getId());
        assertEquals(command.ticker(), actualTransaction.getTicker());
        assertEquals(command.transactionType(), actualTransaction.getTransactionType());
        assertEquals(command.quantity(), actualTransaction.getQuantity());
        assertEquals(command.price(), actualTransaction.getPrice());
        assertEquals(command.currency(), actualTransaction.getCurrency());
        assertEquals(command.transactionDate(), actualTransaction.getTransactionDate());

        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteSuccessWithDomainEventPublishing() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);
                    Transaction savedTransaction = new Transaction(
                            transactionId,
                            inputTransaction.getTicker(),
                            inputTransaction.getTransactionType(),
                            inputTransaction.getQuantity(),
                            inputTransaction.getPrice(),
                            inputTransaction.getFees(),
                            inputTransaction.getCurrency(),
                            inputTransaction.getTransactionDate(),
                            inputTransaction.getNotes(),
                            inputTransaction.getIsActive(),
                            inputTransaction.getIsFractional(),
                            inputTransaction.getFractionalMultiplier(),
                            inputTransaction.getCommissionCurrency(),
                            new java.util.ArrayList<>(inputTransaction.getDomainEvents()) // Mutable copy
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        // Verify that the event publisher was called with the correct event type
        ArgumentCaptor<DomainEvent<Transaction>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        DomainEvent<Transaction> publishedEvent = eventCaptor.getValue();
        assertInstanceOf(TransactionCreatedEvent.class, publishedEvent);
        // The event contains the original transaction from command.toTransaction()
        assertEquals(command.ticker(), publishedEvent.getData().getTicker());
        assertEquals(command.transactionType(), publishedEvent.getData().getTransactionType());
    }

    @Test
    void testExecuteRepositoryFailure() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        RuntimeException exception = new RuntimeException("Database error");

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Uni.createFrom().failure(exception));

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        var failure = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceException.class)
                .getFailure();

        assertEquals(Errors.CreateTransaction.PERSISTENCE_ERROR, ((ServiceException) failure).getError());

        verify(transactionRepository).save(any(Transaction.class));
        // Event publisher should not be called if repository fails
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteEventPublishingFailure() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();
        RuntimeException eventException = new RuntimeException("Event publishing error");

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);
                    Transaction savedTransaction = new Transaction(
                            transactionId,
                            inputTransaction.getTicker(),
                            inputTransaction.getTransactionType(),
                            inputTransaction.getQuantity(),
                            inputTransaction.getPrice(),
                            inputTransaction.getFees(),
                            inputTransaction.getCurrency(),
                            inputTransaction.getTransactionDate(),
                            inputTransaction.getNotes(),
                            inputTransaction.getIsActive(),
                            inputTransaction.getIsFractional(),
                            inputTransaction.getFractionalMultiplier(),
                            inputTransaction.getCommissionCurrency(),
                            new java.util.ArrayList<>(inputTransaction.getDomainEvents()) // Mutable copy
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().failure(eventException));

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        var failure = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ServiceException.class)
                .getFailure();

        assertEquals(Errors.CreateTransaction.PERSISTENCE_ERROR, ((ServiceException) failure).getError());

        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteWithNoEvents() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();
        
        // Create transaction without domain events
        Transaction savedTransaction = new Transaction(
                transactionId,
                command.ticker(),
                command.transactionType(),
                command.quantity(),
                command.price(),
                command.fees(),
                command.currency(),
                command.transactionDate(),
                command.notes(),
                true, // isActive
                command.isFractional(),
                command.fractionalMultiplier(),
                command.commissionCurrency(),
                Collections.emptyList() // No events
        );

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Uni.createFrom().item(savedTransaction));

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        verify(transactionRepository).save(any(Transaction.class));
        // Event publisher should not be called if no events to publish
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteWithComplexTransaction() {
        // Given
        CreateTransactionCommand command = createComplexCommand();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);
                    Transaction savedTransaction = new Transaction(
                            transactionId,
                            inputTransaction.getTicker(),
                            inputTransaction.getTransactionType(),
                            inputTransaction.getQuantity(),
                            inputTransaction.getPrice(),
                            inputTransaction.getFees(),
                            inputTransaction.getCurrency(),
                            inputTransaction.getTransactionDate(),
                            inputTransaction.getNotes(),
                            inputTransaction.getIsActive(),
                            inputTransaction.getIsFractional(),
                            inputTransaction.getFractionalMultiplier(),
                            inputTransaction.getCommissionCurrency(),
                            new java.util.ArrayList<>(inputTransaction.getDomainEvents()) // Mutable copy
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<Transaction> result = useCase.execute(command);

        // Then
        Transaction actualTransaction = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualTransaction);
        assertEquals(transactionId, actualTransaction.getId());
        assertEquals(command.ticker(), actualTransaction.getTicker());
        assertEquals(command.fees(), actualTransaction.getFees());
        assertEquals(command.notes(), actualTransaction.getNotes());
        assertEquals(command.isFractional(), actualTransaction.getIsFractional());
        assertEquals(command.fractionalMultiplier(), actualTransaction.getFractionalMultiplier());
        assertEquals(command.commissionCurrency(), actualTransaction.getCommissionCurrency());

        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    private CreateTransactionCommand createValidCommand() {
        return new CreateTransactionCommand(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                null,
                false,
                BigDecimal.ONE,
                null
        );
    }

    private CreateTransactionCommand createComplexCommand() {
        return new CreateTransactionCommand(
                "MSFT",
                TransactionType.SELL,
                new BigDecimal("5.5"),
                new BigDecimal("420.75"),
                new BigDecimal("9.99"),
                Currency.USD,
                LocalDate.of(2024, 2, 20),
                "Complex sell transaction",
                true,
                new BigDecimal("0.5"),
                Currency.EUR
        );
    }

    /**
     * Helper method to create a saved transaction from command for testing.
     * This simulates what the repository would return after saving.
     */
    private Transaction createSavedTransactionFromCommand(UUID transactionId, CreateTransactionCommand command) {
        // Create a saved transaction with an ID but no domain events (as they would have been processed)
        return new Transaction(
                transactionId,
                command.ticker(),
                command.transactionType(),
                command.quantity(),
                command.price(),
                command.fees(),
                command.currency(),
                command.transactionDate(),
                command.notes(),
                true, // isActive
                command.isFractional(),
                command.fractionalMultiplier(),
                command.commissionCurrency(),
                Collections.emptyList() // No events in saved transaction
        );
    }
}
