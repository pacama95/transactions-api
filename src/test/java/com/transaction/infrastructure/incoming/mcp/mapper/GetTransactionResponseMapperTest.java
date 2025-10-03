package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.incoming.mcp.dto.GetTransactionResponseDto;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionResponseMapperTest {

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private GetTransactionResponseMapperImpl mapper;

    @Test
    void toDto_mapsTransactionCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "AMZN",
                TransactionType.BUY,
                new BigDecimal("8.0"),
                new BigDecimal("3400.0"),
                new BigDecimal("15.0"),
                Currency.USD,
                LocalDate.of(2024, 6, 25),
                "Amazon purchase",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        // When - Don't mock toDto, call it directly
        TransactionDto result = mapper.toDto(transaction);

        // Then
        assertNotNull(result);
        assertEquals(id, result.id());
        assertEquals("AMZN", result.ticker());
        assertEquals(TransactionType.BUY, result.transactionType());
        assertEquals(new BigDecimal("8.0"), result.quantity());
        assertEquals(new BigDecimal("3400.0"), result.price());
    }

    @Test
    void toSuccessDto_wrapsTransactionInSuccessResponse() {
        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.create(
                id,
                "NFLX",
                TransactionType.DIVIDEND,
                new BigDecimal("100.0"),
                new BigDecimal("2.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 6, 25),
                "Dividend payment",
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NASDAQ",
                "USA"
        );

        // When - Don't mock toDto, call it directly  
        GetTransactionResponseDto.Success result = mapper.toSuccessDto(transaction);

        // Then
        assertNotNull(result);
        assertNotNull(result.transaction());
        assertEquals(id, result.transaction().id());
        assertEquals("NFLX", result.transaction().ticker());
        assertEquals(TransactionType.DIVIDEND, result.transaction().transactionType());
    }

    @Test
    void toNotFoundDto_createsNotFoundResponse() {
        // When
        GetTransactionResponseDto.NotFound result = mapper.toNotFoundDto();

        // Then
        assertNotNull(result);
    }

    @Test
    void toErrorDto_createsErrorResponseWithMessage() {
        // Given
        String errorMessage = "Transaction retrieval failed";

        // When
        GetTransactionResponseDto.Error result = mapper.toErrorDto(errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(errorMessage, result.error());
    }
}

