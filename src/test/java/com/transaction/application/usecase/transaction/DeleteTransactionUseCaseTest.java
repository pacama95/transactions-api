package com.transaction.application.usecase.transaction;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.domain.port.output.DomainEventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteTransactionUseCaseTest {
    private TransactionRepository transactionRepository;
    private DomainEventPublisher eventPublisher;
    private DeleteTransactionService useCase;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        useCase = new DeleteTransactionService();
        useCase.transactionRepository = transactionRepository;
        useCase.eventPublisher = eventPublisher;
    }

    @Test
    void testExecuteSuccess() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createTransaction(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<DeleteTransactionUseCase.Result> result = useCase.execute(transactionId);

        DeleteTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.Success.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }

    @Test
    void testExecuteTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().nullItem());

        Uni<DeleteTransactionUseCase.Result> result = useCase.execute(transactionId);

        DeleteTransactionUseCase.Result actual = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.NotFound.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository, never()).deleteById(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteDeleteFailedReturnsError() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createTransaction(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(false));

        DeleteTransactionUseCase.Result actual = useCase.execute(transactionId).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.Error.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteFindByIdFailure() {
        UUID transactionId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("Database error");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().failure(exception));

        DeleteTransactionUseCase.Result actual = useCase.execute(transactionId).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.Error.class, actual);
        DeleteTransactionUseCase.Result.Error error = (DeleteTransactionUseCase.Result.Error) actual;
        assertEquals(Errors.DeleteTransactionsErrors.PERSISTENCE_ERROR, error.error());
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository, never()).deleteById(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testExecuteDeleteByIdFailureReturnsError() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createTransaction(transactionId);
        RuntimeException exception = new RuntimeException("Delete failed");

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().failure(exception));

        DeleteTransactionUseCase.Result actual = useCase.execute(transactionId).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.Error.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testEventPublishingFailureReturnsPublishError() {
        UUID transactionId = UUID.randomUUID();
        Transaction existingTransaction = createTransaction(transactionId);

        when(transactionRepository.findById(transactionId))
                .thenReturn(Uni.createFrom().item(existingTransaction));
        when(transactionRepository.deleteById(transactionId))
                .thenReturn(Uni.createFrom().item(true));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("publish failed")));

        DeleteTransactionUseCase.Result actual = useCase.execute(transactionId).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertInstanceOf(DeleteTransactionUseCase.Result.PublishError.class, actual);
        verify(transactionRepository).findById(transactionId);
        verify(transactionRepository).deleteById(transactionId);
        verify(eventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
    }

    private Transaction createTransaction(UUID id) {
        return Transaction.create(
                id,
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                new BigDecimal("9.99"),
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                "Test transaction",
                true,
                false,
                BigDecimal.ONE,
                null,
                "NYSE",
                "USA"
        );
    }
}
