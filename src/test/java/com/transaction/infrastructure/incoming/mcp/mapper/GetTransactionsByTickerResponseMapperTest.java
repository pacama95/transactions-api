package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.GetTransactionsByTickerResponseDto;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionsByTickerResponseMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private GetTransactionsByTickerResponseMapperImpl mapper;

    @Test
    void toDtoList_mapsMultipleTransactionsCorrectly() {
        // Given
        Transaction transaction1 = Transaction.create(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10.0"),
                new BigDecimal("150.0"),
                new BigDecimal("5.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 1),
                "First purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        Transaction transaction2 = Transaction.create(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("5.0"),
                new BigDecimal("155.0"),
                new BigDecimal("2.5"),
                Currency.USD,
                LocalDate.of(2024, 6, 15),
                "Second purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        List<Transaction> transactions = List.of(transaction1, transaction2);
        
        // Mock the TransactionMapper to convert each Transaction to TransactionDto
        when(transactionMapper.toDto(transaction1)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction1));
        when(transactionMapper.toDto(transaction2)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction2));

        // When
        List<TransactionDto> result = mapper.toDtoList(transactions);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).ticker());
        assertEquals("AAPL", result.get(1).ticker());
        assertEquals(new BigDecimal("10.0"), result.get(0).quantity());
        assertEquals(new BigDecimal("5.0"), result.get(1).quantity());
    }

    @Test
    void toSuccessDto_wrapsTransactionsInSuccessResponse() {
        // Given
        Transaction transaction = Transaction.create(
                UUID.randomUUID(),
                "MSFT",
                TransactionType.BUY,
                new BigDecimal("20.0"),
                new BigDecimal("380.0"),
                new BigDecimal("10.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 20),
                "Microsoft purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        when(transactionMapper.toDto(transaction)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction));
        
        GetTransactionByTickerUseCase.Result.Success success = 
                new GetTransactionByTickerUseCase.Result.Success(List.of(transaction));

        // When
        GetTransactionsByTickerResponseDto.Success result = mapper.toSuccessDto(success);

        // Then
        assertNotNull(result);
        assertNotNull(result.transactions());
        assertEquals(1, result.transactions().size());
        assertEquals("MSFT", result.transactions().get(0).ticker());
    }

    @Test
    void toNotFoundDto_createsNotFoundResponse() {
        // Given
        GetTransactionByTickerUseCase.Result.NotFound notFound = 
                new GetTransactionByTickerUseCase.Result.NotFound();

        // When
        GetTransactionsByTickerResponseDto.NotFound result = mapper.toNotFoundDto(notFound);

        // Then
        assertNotNull(result);
    }

    @Test
    void toErrorDto_createsErrorResponseWithMessage() {
        // Given
        Throwable throwable = new RuntimeException("Database connection failed");
        GetTransactionByTickerUseCase.Result.Error error = 
                new GetTransactionByTickerUseCase.Result.Error(throwable);

        // When
        GetTransactionsByTickerResponseDto.Error result = mapper.toErrorDto(error);

        // Then
        assertNotNull(result);
        assertNotNull(result.error());
        assertEquals("Database connection failed", result.error());
    }
}

