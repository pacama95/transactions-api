package com.transaction.application.usecase.transaction;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GetTransactionsByTickerService Tests")
class GetTransactionsByTickerServiceTest {

    private TransactionRepository transactionRepository;
    private GetTransactionsByTickerService service;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        service = new GetTransactionsByTickerService();
        service.transactionRepository = transactionRepository;
    }

    @Test
    @DisplayName("Should return Success result with transactions when ticker has transactions")
    void testGetByTickerSuccess() {
        // Given
        String ticker = "AAPL";
        List<Transaction> transactions = List.of(
                createTransaction("AAPL", TransactionType.BUY),
                createTransaction("AAPL", TransactionType.SELL)
        );

        when(transactionRepository.findByTicker(ticker))
                .thenReturn(Uni.createFrom().item(transactions));

        // When
        Uni<GetTransactionByTickerUseCase.Result> result = service.getByTicker(ticker);

        // Then
        GetTransactionByTickerUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualResult);
        assertInstanceOf(GetTransactionByTickerUseCase.Result.Success.class, actualResult);

        GetTransactionByTickerUseCase.Result.Success success = (GetTransactionByTickerUseCase.Result.Success) actualResult;
        assertEquals(2, success.transactions().size());
        assertEquals("AAPL", success.transactions().get(0).getTicker());
        assertEquals("AAPL", success.transactions().get(1).getTicker());

        verify(transactionRepository).findByTicker(ticker);
    }

    @Test
    @DisplayName("Should return NotFound result when ticker has no transactions")
    void testGetByTickerNotFound() {
        // Given
        String ticker = "NONEXISTENT";
        List<Transaction> emptyList = List.of();

        when(transactionRepository.findByTicker(ticker))
                .thenReturn(Uni.createFrom().item(emptyList));

        // When
        Uni<GetTransactionByTickerUseCase.Result> result = service.getByTicker(ticker);

        // Then
        GetTransactionByTickerUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualResult);
        assertInstanceOf(GetTransactionByTickerUseCase.Result.NotFound.class, actualResult);

        verify(transactionRepository).findByTicker(ticker);
    }

    @Test
    @DisplayName("Should return Error result when repository fails")
    void testGetByTickerError() {
        // Given
        String ticker = "AAPL";
        RuntimeException repositoryException = new RuntimeException("Database connection failed");

        when(transactionRepository.findByTicker(ticker))
                .thenReturn(Uni.createFrom().failure(repositoryException));

        // When
        Uni<GetTransactionByTickerUseCase.Result> result = service.getByTicker(ticker);

        // Then
        GetTransactionByTickerUseCase.Result actualResult = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertNotNull(actualResult);
        assertInstanceOf(GetTransactionByTickerUseCase.Result.Error.class, actualResult);

        GetTransactionByTickerUseCase.Result.Error error = (GetTransactionByTickerUseCase.Result.Error) actualResult;
        assertEquals(repositoryException, error.throwable());

        verify(transactionRepository).findByTicker(ticker);
    }

    private Transaction createTransaction(String ticker, TransactionType type) {
        return Transaction.create(
                UUID.randomUUID(),
                ticker,
                type,
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
