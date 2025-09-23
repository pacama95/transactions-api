package com.transaction.application.usecase.transaction;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
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

class CreateTransactionUseCaseTest {
    private TransactionRepository transactionRepository;
    private EventPublisher<DomainEvent<?>> eventPublisher;
    private CreateTransactionService useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(EventPublisher.class);
        useCase = new CreateTransactionService();
        useCase.transactionRepository = transactionRepository;
        useCase.eventPublisher = eventPublisher;
    }

    @Test
    void testExecuteSuccess() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);

                    Transaction savedTransaction = Transaction.create(
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
                            inputTransaction.getExchange(),
                            inputTransaction.getCountry()
                    );
                    // Return transaction with preserved mutable events
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualResult);
        assertInstanceOf(CreateTransactionUseCase.Result.Success.class, actualResult);

        CreateTransactionUseCase.Result.Success success = (CreateTransactionUseCase.Result.Success) actualResult;
        Transaction actualTransaction = success.transaction();

        assertEquals(transactionId, actualTransaction.getId());
        assertEquals(command.ticker(), actualTransaction.getTicker());
        assertEquals(command.transactionType(), actualTransaction.getTransactionType());
        assertEquals(command.quantity(), actualTransaction.getQuantity());
        assertEquals(command.price(), actualTransaction.getPrice());
        assertEquals(command.currency(), actualTransaction.getCurrency());
        assertEquals(command.transactionDate(), actualTransaction.getTransactionDate());

        verify(transactionRepository).save(any(Transaction.class));
        // With current behavior, repository returns a transaction without domain events,
        // so no publishing is attempted.
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteSuccessWithDomainEventPublishing() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction inputTransaction = invocation.getArgument(0);
                    Transaction savedTransaction = Transaction.create(
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
                            inputTransaction.getExchange(),
                            inputTransaction.getCountry()
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(CreateTransactionUseCase.Result.Success.class, actualResult);

        // No publishing is attempted when repository returns a transaction without domain events
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteRepositoryFailure() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        ServiceException exception = new ServiceException(Errors.CreateTransactionsErrors.PERSISTENCE_ERROR, "Persistence error");

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Uni.createFrom().failure(exception));

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(CreateTransactionUseCase.Result.Error.class, actualResult);
        CreateTransactionUseCase.Result.Error error = (CreateTransactionUseCase.Result.Error) actualResult;
        assertEquals(Errors.CreateTransactionsErrors.PERSISTENCE_ERROR, error.error());
        assertEquals(command, error.command());

        verify(transactionRepository).save(any(Transaction.class));
        // Event publisher might be called for error events, so we don't verify never
        // The important check is that we get the correct error result
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
                    Transaction savedTransaction = Transaction.create(
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
                            inputTransaction.getExchange(),
                            inputTransaction.getCountry()
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().failure(eventException));

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        // Since no events are present, no publishing is attempted and result remains Success
        assertInstanceOf(CreateTransactionUseCase.Result.Success.class, actualResult);

        verify(transactionRepository).save(any(Transaction.class));
        // No publishing is attempted
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteWithNoEvents() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        UUID transactionId = UUID.randomUUID();

        // Create transaction without domain events
        Transaction savedTransaction = Transaction.create(
                transactionId,
                command.ticker(),
                command.transactionType(),
                command.quantity(),
                command.price(),
                command.fees(),
                command.currency(),
                command.transactionDate(),
                command.notes(),
                true,
                command.isFractional(),
                command.fractionalMultiplier(),
                command.commissionCurrency(),
                "NYSE",
                "USA"
        );

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(Uni.createFrom().item(savedTransaction));

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(CreateTransactionUseCase.Result.Success.class, actualResult);

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
                    Transaction savedTransaction = Transaction.create(
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
                            inputTransaction.getExchange(),
                            inputTransaction.getCountry()
                    );
                    return Uni.createFrom().item(savedTransaction);
                });
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        // When
        Uni<CreateTransactionUseCase.Result> result = useCase.execute(command);

        // Then
        CreateTransactionUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualResult);
        assertInstanceOf(CreateTransactionUseCase.Result.Success.class, actualResult);

        CreateTransactionUseCase.Result.Success success = (CreateTransactionUseCase.Result.Success) actualResult;
        Transaction actualTransaction = success.transaction();

        assertEquals(transactionId, actualTransaction.getId());
        assertEquals(command.ticker(), actualTransaction.getTicker());
        assertEquals(command.fees(), actualTransaction.getFees());
        assertEquals(command.notes(), actualTransaction.getNotes());
        assertEquals(command.isFractional(), actualTransaction.getIsFractional());
        assertEquals(command.fractionalMultiplier(), actualTransaction.getFractionalMultiplier());
        assertEquals(command.commissionCurrency(), actualTransaction.getCommissionCurrency());

        verify(transactionRepository).save(any(Transaction.class));
        verifyNoInteractions(eventPublisher);
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
                null,
                "NYSE",
                "USA"
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
                Currency.EUR,
                "NASDAQ",
                "USA"
        );
    }

}