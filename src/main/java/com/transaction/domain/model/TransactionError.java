package com.transaction.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionError(
        String ticker,
        TransactionType transactionType,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate transactionDate
) {
}
