package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMapperTest {

    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TransactionMapper.class);
    }

    @Test
    void toDto_mapsAllFieldsCorrectly() {
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
                "USA",
                "Apple Inc."
        );

        // When
        TransactionDto dto = mapper.toDto(transaction);

        // Then
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("AAPL", dto.ticker());
        assertEquals(TransactionType.BUY, dto.transactionType());
        assertEquals(new BigDecimal("10.0"), dto.quantity());
        assertEquals(new BigDecimal("150.50"), dto.price());
        assertEquals(new BigDecimal("5.0"), dto.fees());
        assertEquals(Currency.USD, dto.currency());
        assertEquals(LocalDate.of(2024, 6, 15), dto.transactionDate());
        assertEquals("Test transaction", dto.notes());
        assertTrue(dto.isActive());
        assertFalse(dto.isFractional());
        assertEquals(BigDecimal.ONE, dto.fractionalMultiplier());
        assertEquals(Currency.USD, dto.commissionCurrency());
        assertEquals("NASDAQ", dto.exchange());
        assertEquals("USA", dto.country());
        assertEquals("Apple Inc.", dto.companyName());
    }
}

