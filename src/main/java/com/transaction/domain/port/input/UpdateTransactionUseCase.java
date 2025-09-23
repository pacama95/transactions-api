package com.transaction.domain.port.input;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.model.Transaction;
import io.smallrye.mutiny.Uni;

public interface UpdateTransactionUseCase {

    Uni<Result> execute(UpdateTransactionCommand command);

    sealed interface Result permits Result.Success, Result.NotFound, Result.PublishError, Result.Error {
        record Success(Transaction transaction) implements Result {
        }

        record NotFound() implements Result {
        }

        record PublishError(Transaction transaction, Throwable throwable) implements Result {
        }

        record Error(com.transaction.domain.exception.Error error,
                     UpdateTransactionCommand command,
                     Throwable throwable) implements Result {
        }
    }
}


