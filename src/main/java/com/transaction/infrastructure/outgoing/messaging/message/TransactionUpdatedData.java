package com.transaction.infrastructure.outgoing.messaging.message;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload data for transaction updated events.
 * Contains both the previous and new transaction states to allow consumers
 * to properly calculate deltas and apply updates to positions.
 */
@RegisterForReflection
public record TransactionUpdatedData(
        TransactionSnapshot previousTransaction,
        TransactionSnapshot newTransaction
) {
    /**
     * Represents a snapshot of a transaction's state at a point in time.
     */
    @RegisterForReflection
    public record TransactionSnapshot(
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
            Currency commissionCurrency,
            String exchange,
            String country
    ) {
    }
}
