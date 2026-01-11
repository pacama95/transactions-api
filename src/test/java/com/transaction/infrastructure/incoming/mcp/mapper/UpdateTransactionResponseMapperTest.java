package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.UpdateTransactionResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTransactionResponseMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private UpdateTransactionResponseMapperImpl mapper;

    @Test
    void toSuccessDto_mapsTransactionCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "GOOGL",
                TransactionType.BUY,
                new BigDecimal("20.0"),
                new BigDecimal("2800.0"),
                new BigDecimal("10.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 20),
                "Updated transaction",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );
        
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenAnswer(inv -> TransactionDtoTestHelper.createTransactionDto(inv.getArgument(0)));
        
        UpdateTransactionUseCase.Result.Success success = new UpdateTransactionUseCase.Result.Success(transaction);

        // When
        UpdateTransactionResponseDto.Success result = mapper.toSuccessDto(success);

        // Then
        assertNotNull(result);
        assertNotNull(result.transaction());
        assertEquals(id, result.transaction().id());
        assertEquals("GOOGL", result.transaction().ticker());
        assertEquals(new BigDecimal("20.0"), result.transaction().quantity());
    }

    @Test
    void toNotFoundDto_createsNotFoundResponse() {
        // Given
        UpdateTransactionUseCase.Result.NotFound notFound = new UpdateTransactionUseCase.Result.NotFound();

        // When
        UpdateTransactionResponseDto.NotFound result = mapper.toNotFoundDto(notFound);

        // Then
        assertNotNull(result);
    }

    @Test
    void toPublishErrorDto_mapsTransactionAndErrorMessage() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "TSLA",
                TransactionType.BUY,
                new BigDecimal("15.0"),
                new BigDecimal("700.0"),
                new BigDecimal("7.5"),
                Currency.USD,
                LocalDate.of(2024, 6, 20),
                "Tesla purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );
        
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenAnswer(inv -> TransactionDtoTestHelper.createTransactionDto(inv.getArgument(0)));
        
        Throwable throwable = new RuntimeException("Event publishing failed");
        UpdateTransactionUseCase.Result.PublishError publishError = 
                new UpdateTransactionUseCase.Result.PublishError(transaction, throwable);

        // When
        UpdateTransactionResponseDto.PublishError result = mapper.toPublishErrorDto(publishError);

        // Then
        assertNotNull(result);
        assertNotNull(result.transaction());
        assertEquals(id, result.transaction().id());
        assertEquals("TSLA", result.transaction().ticker());
        assertNotNull(result.errorMessage());
        assertEquals("Event publishing failed", result.errorMessage());
    }

    @Test
    void toErrorDto_formatsErrorMessageCorrectly() {
        // Given
        com.transaction.domain.exception.Error error = new com.transaction.domain.exception.Error("UPDATE_FAILED");
        Throwable throwable = new RuntimeException("Database error");
        UpdateTransactionUseCase.Result.Error errorResult = 
                new UpdateTransactionUseCase.Result.Error(error, null, throwable);

        // When
        UpdateTransactionResponseDto.Error result = mapper.toErrorDto(errorResult);

        // Then
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("UPDATE_FAILED"));
        assertTrue(result.error().contains("Database error"));
    }
}

