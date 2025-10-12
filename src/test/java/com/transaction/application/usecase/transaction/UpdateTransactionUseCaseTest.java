package com.transaction.application.usecase.transaction;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.domain.port.output.DomainEventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateTransactionUseCaseTest {
    private TransactionRepository transactionRepository;
    private DomainEventPublisher eventPublisher;
    private UpdateTransactionService useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        useCase = new UpdateTransactionService();
        useCase.transactionRepository = transactionRepository;
        useCase.eventPublisher = eventPublisher;
    }

    @Test
    void testExecuteSuccess() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction newTransaction = createNewTransaction(UUID.randomUUID(), command.ticker(), command.transactionType(), command.quantity(), command.price(), command.fees(), command.currency(), command.transactionDate(), command.notes(), command.isFractional(), command.isFractional(), command.fractionalMultiplier(), command.commissionCurrency(), command.exchange(), command.country());
                    return Uni.createFrom().item(newTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(command);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Success.class, actual);
        Transaction newTransaction = ((UpdateTransactionUseCase.Result.Success) actual).transaction();

        assertNotNull(newTransaction);
        assertNotEquals(transactionId, newTransaction.getId()); // New transaction has different ID
        assertEquals(command.ticker(), newTransaction.getTicker());
        assertEquals(command.transactionType(), newTransaction.getTransactionType());
        assertEquals(command.quantity(), newTransaction.getQuantity());
        assertEquals(command.price(), newTransaction.getPrice());
        assertEquals(command.currency(), newTransaction.getCurrency());
        assertEquals(command.transactionDate(), newTransaction.getTransactionDate());
        assertEquals(command.notes(), newTransaction.getNotes());
        assertEquals(command.isFractional(), newTransaction.getIsFractional());
        assertEquals(command.fractionalMultiplier(), newTransaction.getFractionalMultiplier());
        assertEquals(command.commissionCurrency(), newTransaction.getCommissionCurrency());

        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher, times(2)).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().nullItem());

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(command);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.NotFound.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository, never()).deleteById(any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecutePartialUpdate() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand partialCommand = new UpdateTransactionCommand(
                transactionId,
                "GOOGL",
                null, null, null, null, null, null, null, null, null, null,
                null,
                null
        );

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction newTransaction = invocation.getArgument(0);
                    return Uni.createFrom().item(newTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(partialCommand);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Success.class, actual);
        Transaction newTransaction = ((UpdateTransactionUseCase.Result.Success) actual).transaction();
        assertNotEquals(transactionId, newTransaction.getId()); // New transaction has different ID
        assertEquals("GOOGL", newTransaction.getTicker());
        assertEquals(existingTransaction.getTransactionType(), newTransaction.getTransactionType());
        assertEquals(existingTransaction.getQuantity(), newTransaction.getQuantity());
        assertEquals(existingTransaction.getPrice(), newTransaction.getPrice());
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteRepositoryFailure() {
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        RuntimeException exception = new RuntimeException("Database error");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().failure(exception));

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(command);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Error.class, actual);
        UpdateTransactionUseCase.Result.Error error = (UpdateTransactionUseCase.Result.Error) actual;
        assertEquals(Errors.UpdateTransactionsErrors.PERSISTENCE_ERROR, error.error());
        assertEquals(command, error.command());
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository, never()).deleteById(any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteDeleteFailure() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        RuntimeException exception = new RuntimeException("Delete failed");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().failure(exception));

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(command);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Error.class, actual);
        UpdateTransactionUseCase.Result.Error error = (UpdateTransactionUseCase.Result.Error) actual;
        assertEquals(Errors.UpdateTransactionsErrors.PERSISTENCE_ERROR, error.error());
        assertEquals(command, error.command());
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testSaveFailure() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        RuntimeException exception = new RuntimeException("Save failed");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Uni.createFrom().failure(exception));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        UpdateTransactionUseCase.Result actual = useCase.execute(command).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Error.class, actual);
        UpdateTransactionUseCase.Result.Error error = (UpdateTransactionUseCase.Result.Error) actual;
        assertEquals(Errors.UpdateTransactionsErrors.PERSISTENCE_ERROR, error.error());
        assertEquals(command, error.command());
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testCreatedEventPublishingFailureReturnsPublishError() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction newTransaction = createNewTransaction(UUID.randomUUID(), command.ticker(), command.transactionType(), command.quantity(), command.price(), command.fees(), command.currency(), command.transactionDate(), command.notes(), command.isFractional(), command.isFractional(), command.fractionalMultiplier(), command.commissionCurrency(), command.exchange(), command.country());
                    return Uni.createFrom().item(newTransaction);
                });
        // Delete event succeeds, created event fails
        when(eventPublisher.publish(any(TransactionDeletedEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());
        when(eventPublisher.publish(any(TransactionCreatedEvent.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("publish failed")));

        UpdateTransactionUseCase.Result actual = useCase.execute(command).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.PublishError.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventPublisher).publish(any(TransactionDeletedEvent.class));
        verify(eventPublisher).publish(any(TransactionCreatedEvent.class));
    }

    @Test
    void testBothEventsArePublished() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction newTransaction = createNewTransaction(UUID.randomUUID(), command.ticker(), command.transactionType(), command.quantity(), command.price(), command.fees(), command.currency(), command.transactionDate(), command.notes(), command.isFractional(), command.isFractional(), command.fractionalMultiplier(), command.commissionCurrency(), command.exchange(), command.country());
                    return Uni.createFrom().item(newTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        UpdateTransactionUseCase.Result actual = useCase.execute(command).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Success.class, actual);

        // Verify that a TransactionDeletedEvent was published for the old transaction
        ArgumentCaptor<TransactionDeletedEvent> deletedEventCaptor = ArgumentCaptor.forClass(TransactionDeletedEvent.class);
        verify(eventPublisher).publish(deletedEventCaptor.capture());
        TransactionDeletedEvent deletedEvent = deletedEventCaptor.getValue();
        assertEquals(transactionId, deletedEvent.getData().getId(), "TransactionDeletedEvent should contain the old transaction ID");

        // Verify that a TransactionCreatedEvent was published for the new transaction
        ArgumentCaptor<TransactionCreatedEvent> createdEventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventPublisher).publish(createdEventCaptor.capture());
        TransactionCreatedEvent createdEvent = createdEventCaptor.getValue();
        assertNotEquals(transactionId, createdEvent.getData().getId(), "TransactionCreatedEvent should contain a new transaction ID");

        // Verify repository operations
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(transactionRepository).save(any(Transaction.class));
    }

    private Transaction createNewTransaction(UUID id, String ticker, TransactionType transactionType, BigDecimal quantity, BigDecimal price, BigDecimal commission, Currency currency, LocalDate transactionDate, String notes, boolean isFractional, boolean fractionalMultiplier, BigDecimal fractionalMultiplierValue, Currency fractionalMultiplierCurrency, String exchange, String country) {
        return Transaction.create(
                id,
                ticker,
                transactionType,
                quantity,
                price,
                commission,
                currency,
                transactionDate,
                notes,
                isFractional,
                fractionalMultiplier,
                fractionalMultiplierValue,
                fractionalMultiplierCurrency,
                exchange,
                country
        );
    }

    private Transaction createExistingTransaction(UUID id) {
        return new Transaction(
                id,
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                new BigDecimal("9.99"),
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                "Original notes",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA",
                new java.util.ArrayList<>()
        );
    }

    private UpdateTransactionCommand createUpdateCommand(UUID transactionId) {
        return new UpdateTransactionCommand(
                transactionId,
                "MSFT",
                TransactionType.SELL,
                new BigDecimal("5"),
                new BigDecimal("420.75"),
                new BigDecimal("12.50"),
                Currency.EUR,
                LocalDate.of(2024, 2, 20),
                "Updated transaction notes",
                true,
                new BigDecimal("0.5"),
                Currency.GBP,
                "NASDAQ",
                "USA"
        );
    }
}
