package com.transaction.domain.port.output;

import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import io.smallrye.mutiny.Uni;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port interface for transaction persistence operations
 */
public interface TransactionRepository {

    /**
     * Saves a new transaction
     */
    Uni<Transaction> save(Transaction transaction);

    /**
     * Finds a transaction by its ID
     */
    Uni<Transaction> findById(UUID id);

    /**
     * Finds all transactions for a specific ticker
     */
    Uni<List<Transaction>> findByTicker(String ticker);

    /**
     * Finds all transactions for a specific ticker with limit
     */
    Uni<List<Transaction>> findByTicker(String ticker, Integer limit);

    /**
     * Finds all transactions (active and inactive)
     */
    Uni<List<Transaction>> findAll();

    /**
     * Searches transactions by criteria
     */
    Uni<List<Transaction>> searchTransactions(String ticker, TransactionType type,
                                              LocalDate fromDate, LocalDate toDate);

    /**
     * Searches transactions by criteria with limit
     */
    Uni<List<Transaction>> searchTransactions(String ticker, TransactionType type,
                                              LocalDate fromDate, LocalDate toDate, Integer limit);

    /**
     * Updates an existing transaction
     */
    Uni<Transaction> update(Transaction transaction);

    /**
     * Deletes a transaction by ID
     */
    Uni<Boolean> deleteById(UUID id);

    /**
     * Checks if a transaction exists
     */
    Uni<Boolean> existsById(UUID id);

    /**
     * Counts total transactions
     */
    Uni<Long> countAll();

    /**
     * Counts transactions for a ticker
     */
    Uni<Long> countByTicker(String ticker);
} 