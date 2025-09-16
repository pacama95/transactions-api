package com.transaction.application.usecase.transaction;

import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.domain.port.output.TransactionRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving transactions
 */
@ApplicationScoped
public class GetTransactionsByTickerUseCase implements GetTransactionByTickerUseCase {

    @Inject
    TransactionRepository transactionRepository;

    /**
     * Gets all transactions for a specific ticker
     */
    public Uni<Result> getByTicker(String ticker) {
        return transactionRepository.findByTicker(ticker)
                .map(transactions -> (Result) new Result.Success(transactions))
                .onFailure().recoverWithItem(Result.Error::new);
    }
} 