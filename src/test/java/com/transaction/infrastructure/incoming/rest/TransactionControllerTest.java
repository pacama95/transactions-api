package com.transaction.infrastructure.incoming.rest;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.application.usecase.transaction.GetTransactionUseCase;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.infrastructure.incoming.rest.dto.CreateTransactionRequest;
import com.transaction.infrastructure.incoming.rest.dto.TransactionResponse;
import com.transaction.infrastructure.incoming.rest.dto.UpdateTransactionRequest;
import com.transaction.infrastructure.incoming.rest.mapper.TransactionMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("TransactionController Tests")
class TransactionControllerTest {

    private CreateTransactionUseCase createTransactionUseCase;
    private GetTransactionUseCase getTransactionUseCase;
    private GetTransactionByTickerUseCase getTransactionByTickerUseCase;
    private UpdateTransactionUseCase updateTransactionUseCase;
    private DeleteTransactionUseCase deleteTransactionUseCase;
    private TransactionMapper transactionMapper;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        createTransactionUseCase = mock(CreateTransactionUseCase.class);
        getTransactionUseCase = mock(GetTransactionUseCase.class);
        getTransactionByTickerUseCase = mock(GetTransactionByTickerUseCase.class);
        updateTransactionUseCase = mock(UpdateTransactionUseCase.class);
        deleteTransactionUseCase = mock(DeleteTransactionUseCase.class);
        transactionMapper = mock(TransactionMapper.class);

        controller = new TransactionController();
        controller.createTransactionUseCase = createTransactionUseCase;
        controller.getTransactionUseCase = getTransactionUseCase;
        controller.getTransactionByTickerUseCase = getTransactionByTickerUseCase;
        controller.updateTransactionUseCase = updateTransactionUseCase;
        controller.deleteTransactionUseCase = deleteTransactionUseCase;
        controller.transactionMapper = transactionMapper;
    }

    // ============ CREATE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should return 201 with transaction when create is successful")
    void testCreateTransactionSuccess() {
        // Given
        CreateTransactionRequest request = createValidRequest();
        CreateTransactionCommand command = createValidCommand();
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(transactionMapper.toCreateTransactionCommand(request)).thenReturn(command);
        when(createTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(new CreateTransactionUseCase.Result.Success(transaction)));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Uni<Response> result = controller.createTransaction(request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.CREATED.getStatusCode(), actualResponse.getStatus());
        assertEquals(response, actualResponse.getEntity());

        verify(transactionMapper).toCreateTransactionCommand(request);
        verify(createTransactionUseCase).execute(command);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    @DisplayName("Should return 201 with X-Event-Status header when event publishing fails")
    void testCreateTransactionWithPublishError() {
        // Given
        CreateTransactionRequest request = createValidRequest();
        CreateTransactionCommand command = createValidCommand();
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(transactionMapper.toCreateTransactionCommand(request)).thenReturn(command);
        when(createTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(new CreateTransactionUseCase.Result.PublishError(transaction)));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Uni<Response> result = controller.createTransaction(request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.CREATED.getStatusCode(), actualResponse.getStatus());
        assertEquals("FAILED", actualResponse.getHeaderString("X-Event-Status"));
        assertEquals(response, actualResponse.getEntity());

        verify(createTransactionUseCase).execute(command);
    }

    @Test
    @DisplayName("Should return error response when create fails with persistence error")
    void testCreateTransactionWithPersistenceError() {
        // Given
        CreateTransactionRequest request = createValidRequest();
        CreateTransactionCommand command = createValidCommand();
        CreateTransactionUseCase.Result.Error errorResult =
                new CreateTransactionUseCase.Result.Error(Errors.CreateTransactionsErrors.PERSISTENCE_ERROR, command);
        Response errorResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

        when(transactionMapper.toCreateTransactionCommand(request)).thenReturn(command);
        when(createTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(errorResult));

        // When
        Uni<Response> result = controller.createTransaction(request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), actualResponse.getStatus());

        verify(createTransactionUseCase).execute(command);
    }

    // ============ GET TRANSACTION BY ID TESTS ============

    @Test
    @DisplayName("Should return 200 with transaction when found by ID")
    void testGetTransactionByIdSuccess() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(getTransactionUseCase.getById(transactionId))
                .thenReturn(Uni.createFrom().item(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Uni<Response> result = controller.getTransaction(transactionId);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        assertEquals(response, actualResponse.getEntity());

        verify(getTransactionUseCase).getById(transactionId);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    @DisplayName("Should return 404 when transaction not found by ID")
    void testGetTransactionByIdNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();

        when(getTransactionUseCase.getById(transactionId))
                .thenReturn(Uni.createFrom().nullItem());

        // When
        Uni<Response> result = controller.getTransaction(transactionId);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());

        verify(getTransactionUseCase).getById(transactionId);
        verifyNoInteractions(transactionMapper);
    }

    // ============ GET ALL TRANSACTIONS TESTS ============

    @Test
    @DisplayName("Should return stream of transactions when getting all")
    void testGetAllTransactionsSuccess() {
        // Given
        Transaction transaction1 = createTransaction();
        Transaction transaction2 = createTransaction();
        TransactionResponse response1 = createResponse(transaction1);
        TransactionResponse response2 = createResponse(transaction2);

        when(getTransactionUseCase.getAll())
                .thenReturn(Multi.createFrom().items(transaction1, transaction2));
        when(transactionMapper.toResponse(transaction1)).thenReturn(response1);
        when(transactionMapper.toResponse(transaction2)).thenReturn(response2);

        // When
        Multi<TransactionResponse> result = controller.getAllTransactions();

        // Then
        List<TransactionResponse> responses = result.collect().asList()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(2, responses.size());
        assertEquals(response1, responses.get(0));
        assertEquals(response2, responses.get(1));

        verify(getTransactionUseCase).getAll();
        verify(transactionMapper, times(2)).toResponse(any(Transaction.class));
    }

    @Test
    @DisplayName("Should return empty stream when no transactions exist")
    void testGetAllTransactionsEmpty() {
        // Given
        when(getTransactionUseCase.getAll())
                .thenReturn(Multi.createFrom().empty());

        // When
        Multi<TransactionResponse> result = controller.getAllTransactions();

        // Then
        List<TransactionResponse> responses = result.collect().asList()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(0, responses.size());

        verify(getTransactionUseCase).getAll();
    }

    // ============ GET TRANSACTIONS BY TICKER TESTS ============

    @Test
    @DisplayName("Should return 200 with transactions when ticker has transactions")
    void testGetTransactionsByTickerSuccess() {
        // Given
        String ticker = "AAPL";
        List<Transaction> transactions = List.of(createTransaction());
        List<TransactionResponse> responses = List.of(createResponse(createTransaction()));

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.Success(transactions)));
        when(transactionMapper.toResponses(transactions)).thenReturn(responses);

        // When
        Uni<Response> result = controller.getTransactionsByTicker(ticker);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        assertEquals(responses, actualResponse.getEntity());

        verify(getTransactionByTickerUseCase).getByTicker(ticker);
        verify(transactionMapper).toResponses(transactions);
    }

    @Test
    @DisplayName("Should return 404 when ticker has no transactions")
    void testGetTransactionsByTickerNotFound() {
        // Given
        String ticker = "NONEXISTENT";

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.NotFound()));

        // When
        Uni<Response> result = controller.getTransactionsByTicker(ticker);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());

        verify(getTransactionByTickerUseCase).getByTicker(ticker);
    }

    @Test
    @DisplayName("Should return 500 when repository fails for ticker search")
    void testGetTransactionsByTickerError() {
        // Given
        String ticker = "AAPL";
        RuntimeException exception = new RuntimeException("Database error");

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.Error(exception)));

        // When
        Uni<Response> result = controller.getTransactionsByTicker(ticker);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), actualResponse.getStatus());

        verify(getTransactionByTickerUseCase).getByTicker(ticker);
    }

    // ============ SEARCH TRANSACTIONS TESTS ============

    @Test
    @DisplayName("Should return transactions matching all search filters")
    void testSearchTransactionsWithAllFilters() {
        // Given
        String ticker = "AAPL";
        TransactionType type = TransactionType.BUY;
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 12, 31);
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(getTransactionUseCase.searchTransactions(ticker, type, fromDate, toDate))
                .thenReturn(Multi.createFrom().item(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Multi<TransactionResponse> result = controller.searchTransactions(ticker, type, fromDate, toDate);

        // Then
        List<TransactionResponse> responses = result.collect().asList()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(1, responses.size());
        assertEquals(response, responses.get(0));

        verify(getTransactionUseCase).searchTransactions(ticker, type, fromDate, toDate);
    }

    @Test
    @DisplayName("Should return all transactions when no filters provided")
    void testSearchTransactionsWithNoFilters() {
        // Given
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(getTransactionUseCase.searchTransactions(null, null, null, null))
                .thenReturn(Multi.createFrom().item(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Multi<TransactionResponse> result = controller.searchTransactions(null, null, null, null);

        // Then
        List<TransactionResponse> responses = result.collect().asList()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(1, responses.size());

        verify(getTransactionUseCase).searchTransactions(null, null, null, null);
    }

    // ============ UPDATE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should return 200 with updated transaction when update is successful")
    void testUpdateTransactionSuccess() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionRequest request = createUpdateRequest();
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(transactionMapper.toUpdateTransactionCommand(transactionId, request)).thenReturn(command);
        when(updateTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.Success(transaction)));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Uni<Response> result = controller.updateTransaction(transactionId, request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        assertEquals(response, actualResponse.getEntity());

        verify(updateTransactionUseCase).execute(command);
    }

    @Test
    @DisplayName("Should return 404 when transaction to update is not found")
    void testUpdateTransactionNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionRequest request = createUpdateRequest();
        UpdateTransactionCommand command = createUpdateCommand(transactionId);

        when(transactionMapper.toUpdateTransactionCommand(transactionId, request)).thenReturn(command);
        when(updateTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.NotFound()));

        // When
        Uni<Response> result = controller.updateTransaction(transactionId, request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());

        verify(updateTransactionUseCase).execute(command);
    }

    @Test
    @DisplayName("Should return 200 with X-Event-Status header when update event publishing fails")
    void testUpdateTransactionWithPublishError() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionRequest request = createUpdateRequest();
        UpdateTransactionCommand command = createUpdateCommand(transactionId);
        Transaction transaction = createTransaction();
        TransactionResponse response = createResponse(transaction);

        when(transactionMapper.toUpdateTransactionCommand(transactionId, request)).thenReturn(command);
        when(updateTransactionUseCase.execute(command))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.PublishError(transaction)));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        // When
        Uni<Response> result = controller.updateTransaction(transactionId, request);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.OK.getStatusCode(), actualResponse.getStatus());
        assertEquals("FAILED", actualResponse.getHeaderString("X-Event-Status"));

        verify(updateTransactionUseCase).execute(command);
    }

    // ============ DELETE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should return 204 when transaction is deleted successfully")
    void testDeleteTransactionSuccess() {
        // Given
        UUID transactionId = UUID.randomUUID();

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.Success()));

        // When
        Uni<Response> result = controller.deleteTransaction(transactionId);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), actualResponse.getStatus());

        verify(deleteTransactionUseCase).execute(transactionId);
    }

    @Test
    @DisplayName("Should return 404 when transaction to delete is not found")
    void testDeleteTransactionNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.NotFound()));

        // When
        Uni<Response> result = controller.deleteTransaction(transactionId);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), actualResponse.getStatus());

        verify(deleteTransactionUseCase).execute(transactionId);
    }

    @Test
    @DisplayName("Should return 204 with X-Event-Status header when delete event publishing fails")
    void testDeleteTransactionWithPublishError() {
        // Given
        UUID transactionId = UUID.randomUUID();

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.PublishError()));

        // When
        Uni<Response> result = controller.deleteTransaction(transactionId);

        // Then
        Response actualResponse = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), actualResponse.getStatus());
        assertEquals("FAILED", actualResponse.getHeaderString("X-Event-Status"));

        verify(deleteTransactionUseCase).execute(transactionId);
    }

    // ============ COUNT TESTS ============

    @Test
    @DisplayName("Should return total transaction count")
    void testGetTransactionCount() {
        // Given
        Long expectedCount = 42L;

        when(getTransactionUseCase.countAll())
                .thenReturn(Uni.createFrom().item(expectedCount));

        // When
        Uni<Long> result = controller.getTransactionCount();

        // Then
        Long actualCount = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedCount, actualCount);

        verify(getTransactionUseCase).countAll();
    }

    @Test
    @DisplayName("Should return transaction count by ticker")
    void testGetTransactionCountByTicker() {
        // Given
        String ticker = "AAPL";
        Long expectedCount = 15L;

        when(getTransactionUseCase.countByTicker(ticker))
                .thenReturn(Uni.createFrom().item(expectedCount));

        // When
        Uni<Long> result = controller.getTransactionCountByTicker(ticker);

        // Then
        Long actualCount = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedCount, actualCount);

        verify(getTransactionUseCase).countByTicker(ticker);
    }

    // ============ HELPER METHODS ============

    private CreateTransactionRequest createValidRequest() {
        return new CreateTransactionRequest(
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

    private UpdateTransactionRequest createUpdateRequest() {
        return new UpdateTransactionRequest(
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

    private UpdateTransactionCommand createUpdateCommand(UUID transactionId) {
        return new UpdateTransactionCommand(
                transactionId,
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

    private Transaction createTransaction() {
        return Transaction.create(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.now(),
                null,
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }

    private TransactionResponse createResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTicker(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFees(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getIsFractional(),
                transaction.getFractionalMultiplier(),
                transaction.getCommissionCurrency(),
                transaction.getExchange(),
                transaction.getCountry()
        );
    }
}
