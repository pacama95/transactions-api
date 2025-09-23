package com.transaction.application.usecase.transaction;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.domain.port.output.EventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateTransactionUseCaseTest {
    private TransactionRepository transactionRepository;
    private EventPublisher<DomainEvent<?>> eventPublisher;
    private UpdateTransactionService useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(EventPublisher.class);
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
        when(transactionRepository.update(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction updated = invocation.getArgument(0);
                    return Uni.createFrom().item(updated);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(command);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Success.class, actual);
        Transaction updated = ((UpdateTransactionUseCase.Result.Success) actual).transaction();

        assertNotNull(updated);
        assertEquals(command.ticker(), updated.getTicker());
        assertEquals(command.transactionType(), updated.getTransactionType());
        assertEquals(command.quantity(), updated.getQuantity());
        assertEquals(command.price(), updated.getPrice());
        assertEquals(command.currency(), updated.getCurrency());
        assertEquals(command.transactionDate(), updated.getTransactionDate());
        assertEquals(command.notes(), updated.getNotes());
        assertEquals(command.isFractional(), updated.getIsFractional());
        assertEquals(command.fractionalMultiplier(), updated.getFractionalMultiplier());
        assertEquals(command.commissionCurrency(), updated.getCommissionCurrency());

        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).update(any(Transaction.class));
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
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
        verify(transactionRepository, never()).update(any());
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
        when(transactionRepository.update(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction updated = invocation.getArgument(0);
                    return Uni.createFrom().item(updated);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<UpdateTransactionUseCase.Result> result = useCase.execute(partialCommand);

        UpdateTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.Success.class, actual);
        Transaction updated = ((UpdateTransactionUseCase.Result.Success) actual).transaction();
        assertEquals("GOOGL", updated.getTicker());
        assertEquals(existingTransaction.getTransactionType(), updated.getTransactionType());
        assertEquals(existingTransaction.getQuantity(), updated.getQuantity());
        assertEquals(existingTransaction.getPrice(), updated.getPrice());
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
        verify(transactionRepository, never()).update(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteUpdateFailure() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        RuntimeException exception = new RuntimeException("Update failed");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.update(any(Transaction.class)))
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
        verify(transactionRepository).update(any(Transaction.class));
    }

    @Test
    void testEventPublishingFailureReturnsPublishError() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createExistingTransaction(transactionId);
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.update(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction updated = invocation.getArgument(0);
                    return Uni.createFrom().item(updated);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("publish failed")));

        UpdateTransactionUseCase.Result actual = useCase.execute(command).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(UpdateTransactionUseCase.Result.PublishError.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).update(any(Transaction.class));
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }

    private Transaction createExistingTransaction(UUID id) {
        return Transaction.create(
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
                "USA"
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
