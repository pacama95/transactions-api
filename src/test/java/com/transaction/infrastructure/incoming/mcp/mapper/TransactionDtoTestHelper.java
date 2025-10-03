package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;

/**
 * Helper class to create TransactionDto objects for testing
 */
public class TransactionDtoTestHelper {

    public static TransactionDto createTransactionDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getTicker(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFees(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getIsActive(),
                transaction.getIsFractional(),
                transaction.getFractionalMultiplier(),
                transaction.getCommissionCurrency(),
                transaction.getExchange(),
                transaction.getCountry()
        );
    }
}

