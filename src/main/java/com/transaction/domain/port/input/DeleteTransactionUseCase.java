package com.transaction.domain.port.input;

import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface DeleteTransactionUseCase {

    Uni<Result> execute(UUID id);

    sealed interface Result permits Result.Success, Result.NotFound, Result.PublishError, Result.Error {
        record Success(UUID id) implements Result { }

        record NotFound(UUID id) implements Result { }

        record PublishError(UUID id, Throwable throwable) implements Result { }

        record Error(com.transaction.domain.exception.Error error, UUID id, Throwable throwable) implements Result { }
    }
}


