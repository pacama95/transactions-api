package com.transaction.application.command;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionCommand(
        UUID transactionId,
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
        String country,
        String companyName
) {
}
