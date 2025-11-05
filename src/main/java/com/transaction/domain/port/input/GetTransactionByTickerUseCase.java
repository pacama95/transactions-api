package com.transaction.domain.port.input;

import com.transaction.domain.model.Transaction;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface GetTransactionByTickerUseCase {

    Uni<Result> getByTicker(String ticker);

    Uni<Result> getByTicker(String ticker, Integer limit);

    sealed interface Result {
        record Success(List<Transaction> transactions) implements Result {
        }

        record Error(Throwable throwable) implements Result {
        }

        record NotFound() implements Result {
        }
    }
}
