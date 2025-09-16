package com.transaction.application.command;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionCommand(
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

    public Transaction toTransaction() {
        return new Transaction(
                ticker,
                transactionType,
                quantity,
                price,
                fees,
                currency,
                transactionDate,
                notes,
                true,
                isFractional,
                fractionalMultiplier,
                commissionCurrency
        );
    }
}
