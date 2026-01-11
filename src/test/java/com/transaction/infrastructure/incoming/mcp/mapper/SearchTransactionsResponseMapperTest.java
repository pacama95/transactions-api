package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.incoming.mcp.dto.SearchTransactionsResponseDto;
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
class SearchTransactionsResponseMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private SearchTransactionsResponseMapperImpl mapper;

    @Test
    void toDtoList_mapsMultipleTransactionsFromDifferentTickers() {
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
                "Apple purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );

        Transaction transaction2 = Transaction.create(
                UUID.randomUUID(),
                "GOOGL",
                TransactionType.BUY,
                new BigDecimal("5.0"),
                new BigDecimal("2800.0"),
                new BigDecimal("10.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 10),
                "Google purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );

        Transaction transaction3 = Transaction.create(
                UUID.randomUUID(),
                "TSLA",
                TransactionType.SELL,
                new BigDecimal("15.0"),
                new BigDecimal("700.0"),
                new BigDecimal("7.5"),
                Currency.USD,
                LocalDate.of(2024, 6, 15),
                "Tesla sale",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );

        List<Transaction> transactions = List.of(transaction1, transaction2, transaction3);
        
        // Mock the TransactionMapper to convert each Transaction to TransactionDto
        when(transactionMapper.toDto(transaction1)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction1));
        when(transactionMapper.toDto(transaction2)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction2));
        when(transactionMapper.toDto(transaction3)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction3));

        // When
        List<TransactionDto> result = mapper.toDtoList(transactions);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("AAPL", result.get(0).ticker());
        assertEquals("GOOGL", result.get(1).ticker());
        assertEquals("TSLA", result.get(2).ticker());
        assertEquals(TransactionType.BUY, result.get(0).transactionType());
        assertEquals(TransactionType.BUY, result.get(1).transactionType());
        assertEquals(TransactionType.SELL, result.get(2).transactionType());
    }

    @Test
    void toSuccessDto_wrapsTransactionsInSuccessResponse() {
        // Given
        Transaction transaction1 = Transaction.create(
                UUID.randomUUID(),
                "MSFT",
                TransactionType.BUY,
                new BigDecimal("20.0"),
                new BigDecimal("380.0"),
                new BigDecimal("10.0"),
                Currency.USD,
                LocalDate.of(2024, 5, 1),
                "May purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );

        Transaction transaction2 = Transaction.create(
                UUID.randomUUID(),
                "MSFT",
                TransactionType.DIVIDEND,
                new BigDecimal("20.0"),
                new BigDecimal("0.75"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 6, 1),
                "Quarterly dividend",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );

        List<Transaction> transactions = List.of(transaction1, transaction2);
        
        when(transactionMapper.toDto(transaction1)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction1));
        when(transactionMapper.toDto(transaction2)).thenReturn(TransactionDtoTestHelper.createTransactionDto(transaction2));

        // When
        SearchTransactionsResponseDto.Success result = mapper.toSuccessDto(transactions);

        // Then
        assertNotNull(result);
        assertNotNull(result.transactions());
        assertEquals(2, result.transactions().size());
        assertEquals("MSFT", result.transactions().get(0).ticker());
        assertEquals("MSFT", result.transactions().get(1).ticker());
        assertEquals(TransactionType.BUY, result.transactions().get(0).transactionType());
        assertEquals(TransactionType.DIVIDEND, result.transactions().get(1).transactionType());
    }

    @Test
    void toErrorDto_createsErrorResponseWithMessage() {
        // Given
        String errorMessage = "Search query execution failed";

        // When
        SearchTransactionsResponseDto.Error result = mapper.toErrorDto(errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(errorMessage, result.error());
    }
}

