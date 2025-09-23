package com.transaction.infrastructure.incoming.rest.mapper;

import com.transaction.domain.exception.Error;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import jakarta.ws.rs.core.Response;

public class ErrorMapper {

    public static Response mapToResponse(CreateTransactionUseCase.Result.Error errorResult) {
        Error createTransactionError = errorResult.error();

        if (createTransactionError == Errors.CreateTransactionsErrors.INVALID_INPUT) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else if (createTransactionError == Errors.CreateTransactionsErrors.NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static Response mapToResponse(UpdateTransactionUseCase.Result.Error errorResult) {
        Error updateError = errorResult.error();

        if (updateError == Errors.UpdateTransactionsErrors.INVALID_INPUT) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else if (updateError == Errors.UpdateTransactionsErrors.NOT_FOUND) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
