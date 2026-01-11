package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.CreateTransactionResponseDto;
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
class CreateTransactionResponseMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private CreateTransactionResponseMapperImpl mapper;

    @Test
    void toSuccessDto_mapsTransactionCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10.0"),
                new BigDecimal("150.50"),
                new BigDecimal("5.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 15),
                "Test transaction",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );
        
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenAnswer(inv -> TransactionDtoTestHelper.createTransactionDto(inv.getArgument(0)));
        
        CreateTransactionUseCase.Result.Success success = new CreateTransactionUseCase.Result.Success(transaction);

        // When
        CreateTransactionResponseDto.Success result = mapper.toSuccessDto(success);

        // Then
        assertNotNull(result);
        assertNotNull(result.transaction());
        assertEquals(id, result.transaction().id());
        assertEquals("AAPL", result.transaction().ticker());
        assertEquals(TransactionType.BUY, result.transaction().transactionType());
    }

    @Test
    void toPublishErrorDto_mapsTransactionAndErrorMessage() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "MSFT",
                TransactionType.SELL,
                new BigDecimal("5.0"),
                new BigDecimal("300.0"),
                new BigDecimal("2.5"),
                Currency.USD,
                LocalDate.of(2024, 6, 15),
                "Sell transaction",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA", "Test Company"
        );
        
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenAnswer(inv -> TransactionDtoTestHelper.createTransactionDto(inv.getArgument(0)));
        
        Throwable throwable = new RuntimeException("Failed to publish event");
        CreateTransactionUseCase.Result.PublishError publishError = 
                new CreateTransactionUseCase.Result.PublishError(transaction, throwable);

        // When
        CreateTransactionResponseDto.PublishError result = mapper.toPublishErrorDto(publishError);

        // Then
        assertNotNull(result);
        assertNotNull(result.transaction());
        assertEquals(id, result.transaction().id());
        assertEquals("MSFT", result.transaction().ticker());
        assertNotNull(result.errorMessage());
        assertEquals("Failed to publish event", result.errorMessage());
    }

    @Test
    void toErrorDto_formatsErrorMessageWithCodeAndThrowable() {
        // Given
        com.transaction.domain.exception.Error error = new com.transaction.domain.exception.Error("VALIDATION_ERROR");
        Throwable throwable = new RuntimeException("Invalid input");
        CreateTransactionUseCase.Result.Error errorResult =
                new CreateTransactionUseCase.Result.Error(error, null, throwable);

        // When
        CreateTransactionResponseDto.Error result = mapper.toErrorDto(errorResult);

        // Then
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("VALIDATION_ERROR"));
        assertTrue(result.error().contains("Invalid input"));
    }
}

