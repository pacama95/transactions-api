package com.transaction.domain.port.input;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.domain.model.Transaction;
import io.smallrye.mutiny.Uni;

public interface CreateTransactionUseCase {

    Uni<Result> execute(CreateTransactionCommand command);

    sealed interface Result {
        record Success(Transaction transaction) implements Result {
        }

        record PublishError(Transaction transaction, Throwable throwable) implements Result {
        }

        record Error(com.transaction.domain.exception.Error error, CreateTransactionCommand command,
                     Throwable throwable) implements Result {
        }
    }
}
