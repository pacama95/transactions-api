package com.transaction.domain.model;

public record TransactionUpdateData(
        Transaction previousTransaction,
        Transaction newTransaction) {
}

