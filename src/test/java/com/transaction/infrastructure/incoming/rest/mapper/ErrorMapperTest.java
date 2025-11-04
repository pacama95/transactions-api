package com.transaction.infrastructure.incoming.rest.mapper;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ErrorMapper Tests")
class ErrorMapperTest {

    // ============ CREATE TRANSACTION ERROR MAPPING TESTS ============

    @Test
    @DisplayName("Should map create transaction INVALID_INPUT error to 400 BAD_REQUEST")
    void testMapCreateTransactionInvalidInputError() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        CreateTransactionUseCase.Result.Error errorResult =
                new CreateTransactionUseCase.Result.Error(Errors.CreateTransactionsErrors.INVALID_INPUT, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Should map create transaction NOT_FOUND error to 404 NOT_FOUND")
    void testMapCreateTransactionNotFoundError() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        CreateTransactionUseCase.Result.Error errorResult =
                new CreateTransactionUseCase.Result.Error(Errors.CreateTransactionsErrors.NOT_FOUND, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Should map create transaction PERSISTENCE_ERROR to 500 INTERNAL_SERVER_ERROR")
    void testMapCreateTransactionPersistenceError() {
        // Given
        CreateTransactionCommand command = createValidCommand();
        CreateTransactionUseCase.Result.Error errorResult =
                new CreateTransactionUseCase.Result.Error(Errors.CreateTransactionsErrors.PERSISTENCE_ERROR, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    // ============ UPDATE TRANSACTION ERROR MAPPING TESTS ============

    @Test
    @DisplayName("Should map update transaction INVALID_INPUT error to 400 BAD_REQUEST")
    void testMapUpdateTransactionInvalidInputError() {
        // Given
        UpdateTransactionCommand command = createValidUpdateCommand();
        UpdateTransactionUseCase.Result.Error errorResult =
                new UpdateTransactionUseCase.Result.Error(Errors.UpdateTransactionsErrors.INVALID_INPUT, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Should map update transaction NOT_FOUND error to 404 NOT_FOUND")
    void testMapUpdateTransactionNotFoundError() {
        // Given
        UpdateTransactionCommand command = createValidUpdateCommand();
        UpdateTransactionUseCase.Result.Error errorResult =
                new UpdateTransactionUseCase.Result.Error(Errors.UpdateTransactionsErrors.NOT_FOUND, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Should map update transaction PERSISTENCE_ERROR to 500 INTERNAL_SERVER_ERROR")
    void testMapUpdateTransactionPersistenceError() {
        // Given
        UpdateTransactionCommand command = createValidUpdateCommand();
        UpdateTransactionUseCase.Result.Error errorResult =
                new UpdateTransactionUseCase.Result.Error(Errors.UpdateTransactionsErrors.PERSISTENCE_ERROR, command);

        // When
        Response response = ErrorMapper.mapToResponse(errorResult);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    // ============ HELPER METHODS ============

    private CreateTransactionCommand createValidCommand() {
        return new CreateTransactionCommand(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.now(),
                null,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }

    private UpdateTransactionCommand createValidUpdateCommand() {
        return new UpdateTransactionCommand(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.SELL,
                new BigDecimal("5"),
                new BigDecimal("160.00"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.now(),
                null,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }
}
