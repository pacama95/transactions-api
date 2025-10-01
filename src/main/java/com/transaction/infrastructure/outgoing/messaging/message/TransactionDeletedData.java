package com.transaction.infrastructure.outgoing.messaging.message;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload data for transaction deleted events.
 */
@RegisterForReflection
public record TransactionDeletedData(
        UUID id,
        String ticker,
        TransactionType transactionType,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fees,
        Currency currency,
        LocalDate transactionDate,
        String notes,
        Boolean isFractional,
        BigDecimal fractionalMultiplier,
        Currency commissionCurrency
) {
}
