package com.transaction.infrastructure.incoming.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.application.usecase.transaction.GetTransactionUseCase;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.converter.ParameterConversionService;
import com.transaction.infrastructure.incoming.mcp.dto.*;
import com.transaction.infrastructure.incoming.mcp.mapper.*;
import io.quarkiverse.mcp.server.ToolCallException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
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

@DisplayName("PortfolioMcpServer Tests")
class PortfolioMcpServerTest {

    private ObjectMapper objectMapper;
    private CreateTransactionUseCase createTransactionUseCase;
    private GetTransactionUseCase getTransactionUseCase;
    private UpdateTransactionUseCase updateTransactionUseCase;
    private DeleteTransactionUseCase deleteTransactionUseCase;
    private GetTransactionByTickerUseCase getTransactionByTickerUseCase;
    private ParameterConversionService parameterConversionService;
    private CreateTransactionResponseMapper createTransactionResponseMapper;
    private UpdateTransactionResponseMapper updateTransactionResponseMapper;
    private DeleteTransactionResponseMapper deleteTransactionResponseMapper;
    private GetTransactionResponseMapper getTransactionResponseMapper;
    private GetTransactionsByTickerResponseMapper getTransactionsByTickerResponseMapper;
    private SearchTransactionsResponseMapper searchTransactionsResponseMapper;
    private PortfolioMcpServer mcpServer;

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        createTransactionUseCase = mock(CreateTransactionUseCase.class);
        getTransactionUseCase = mock(GetTransactionUseCase.class);
        updateTransactionUseCase = mock(UpdateTransactionUseCase.class);
        deleteTransactionUseCase = mock(DeleteTransactionUseCase.class);
        getTransactionByTickerUseCase = mock(GetTransactionByTickerUseCase.class);
        parameterConversionService = mock(ParameterConversionService.class);
        createTransactionResponseMapper = mock(CreateTransactionResponseMapper.class);
        updateTransactionResponseMapper = mock(UpdateTransactionResponseMapper.class);
        deleteTransactionResponseMapper = mock(DeleteTransactionResponseMapper.class);
        getTransactionResponseMapper = mock(GetTransactionResponseMapper.class);
        getTransactionsByTickerResponseMapper = mock(GetTransactionsByTickerResponseMapper.class);
        searchTransactionsResponseMapper = mock(SearchTransactionsResponseMapper.class);

        mcpServer = new PortfolioMcpServer();
        mcpServer.objectMapper = objectMapper;
        mcpServer.createTransactionUseCase = createTransactionUseCase;
        mcpServer.getTransactionUseCase = getTransactionUseCase;
        mcpServer.updateTransactionUseCase = updateTransactionUseCase;
        mcpServer.deleteTransactionUseCase = deleteTransactionUseCase;
        mcpServer.getTransactionByTickerUseCase = getTransactionByTickerUseCase;
        mcpServer.parameterConversionService = parameterConversionService;
        mcpServer.createTransactionResponseMapper = createTransactionResponseMapper;
        mcpServer.updateTransactionResponseMapper = updateTransactionResponseMapper;
        mcpServer.deleteTransactionResponseMapper = deleteTransactionResponseMapper;
        mcpServer.getTransactionResponseMapper = getTransactionResponseMapper;
        mcpServer.getTransactionsByTickerResponseMapper = getTransactionsByTickerResponseMapper;
        mcpServer.searchTransactionsResponseMapper = searchTransactionsResponseMapper;
    }

    // ============ CREATE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should create transaction successfully and return JSON response")
    void testCreateTransactionSuccess() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        CreateTransactionResponseDto.Success successDto = new CreateTransactionResponseDto.Success(transactionDto);
        String expectedJson = "{\"status\":\"success\"}";

        setupParameterConversions();
        when(createTransactionUseCase.execute(any(CreateTransactionCommand.class)))
                .thenReturn(Uni.createFrom().item(new CreateTransactionUseCase.Result.Success(transaction)));
        when(createTransactionResponseMapper.toSuccessDto(any(CreateTransactionUseCase.Result.Success.class)))
                .thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.createTransaction(
                "AAPL", "BUY", "10", "150.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-15",
                null, "NYSE", "USA"
        );

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(createTransactionUseCase).execute(any(CreateTransactionCommand.class));
        verify(objectMapper).writeValueAsString(successDto);
    }

    @Test
    @DisplayName("Should handle publish error when creating transaction")
    void testCreateTransactionWithPublishError() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        CreateTransactionResponseDto.PublishError publishErrorDto =
                new CreateTransactionResponseDto.PublishError(transactionDto, "Event publishing failed");
        String expectedJson = "{\"status\":\"publish_error\"}";

        setupParameterConversions();
        when(createTransactionUseCase.execute(any(CreateTransactionCommand.class)))
                .thenReturn(Uni.createFrom().item(new CreateTransactionUseCase.Result.PublishError(transaction)));
        when(createTransactionResponseMapper.toPublishErrorDto(any(CreateTransactionUseCase.Result.PublishError.class)))
                .thenReturn(publishErrorDto);
        when(objectMapper.writeValueAsString(publishErrorDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.createTransaction(
                "AAPL", "BUY", "10", "150.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-15",
                null, "NYSE", "USA"
        );

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(createTransactionUseCase).execute(any(CreateTransactionCommand.class));
    }

    @Test
    @DisplayName("Should throw ToolCallException when transaction type is invalid")
    void testCreateTransactionWithInvalidType() {
        // Given
        when(parameterConversionService.convert("INVALID", "type"))
                .thenThrow(new ToolCallException("Invalid TransactionType value"));

        // When/Then
        assertThrows(ToolCallException.class, () -> mcpServer.createTransaction(
                "AAPL", "INVALID", "10", "150.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-15",
                null, "NYSE", "USA"
        ));
    }

    // ============ GET TRANSACTION TESTS ============

    @Test
    @DisplayName("Should get transaction by ID and return JSON response")
    void testGetTransactionSuccess() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        GetTransactionResponseDto.Success successDto = new GetTransactionResponseDto.Success(transactionDto);
        String expectedJson = "{\"transaction\":{\"id\":\"" + transactionId + "\"}}";

        when(getTransactionUseCase.getById(transactionId))
                .thenReturn(Uni.createFrom().item(transaction));
        when(getTransactionResponseMapper.toSuccessDto(transaction)).thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.getTransaction(transactionId.toString());

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(getTransactionUseCase).getById(transactionId);
        verify(objectMapper).writeValueAsString(successDto);
    }

    @Test
    @DisplayName("Should throw ToolCallException when UUID format is invalid for get")
    void testGetTransactionWithInvalidUUID() {
        // Given
        String invalidUuid = "not-a-uuid";

        // When
        Uni<String> result = mcpServer.getTransaction(invalidUuid);

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ToolCallException.class);
    }

    // ============ UPDATE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should update transaction successfully and return JSON response")
    void testUpdateTransactionSuccess() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        UpdateTransactionResponseDto.Success successDto = new UpdateTransactionResponseDto.Success(transactionDto);
        String expectedJson = "{\"status\":\"success\"}";

        setupParameterConversions();
        when(updateTransactionUseCase.execute(any(UpdateTransactionCommand.class)))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.Success(transaction)));
        when(updateTransactionResponseMapper.toSuccessDto(any(UpdateTransactionUseCase.Result.Success.class)))
                .thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.updateTransaction(
                transactionId.toString(), "AAPL", "SELL", "5", "160.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-20",
                "Updated notes", "NYSE", "USA"
        );

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(updateTransactionUseCase).execute(any(UpdateTransactionCommand.class));
    }

    @Test
    @DisplayName("Should return NotFound response when updating non-existent transaction")
    void testUpdateTransactionNotFound() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionResponseDto.NotFound notFoundDto = new UpdateTransactionResponseDto.NotFound("Transaction not found");
        String expectedJson = "{\"status\":\"not_found\"}";

        setupParameterConversions();
        when(updateTransactionUseCase.execute(any(UpdateTransactionCommand.class)))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.NotFound()));
        when(updateTransactionResponseMapper.toNotFoundDto(any(UpdateTransactionUseCase.Result.NotFound.class)))
                .thenReturn(notFoundDto);
        when(objectMapper.writeValueAsString(notFoundDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.updateTransaction(
                transactionId.toString(), "AAPL", "SELL", "5", "160.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-20",
                null, "NYSE", "USA"
        );

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(updateTransactionUseCase).execute(any(UpdateTransactionCommand.class));
    }

    @Test
    @DisplayName("Should handle publish error when updating transaction")
    void testUpdateTransactionWithPublishError() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        UpdateTransactionResponseDto.PublishError publishErrorDto =
                new UpdateTransactionResponseDto.PublishError(transactionDto, "Event publishing failed");
        String expectedJson = "{\"status\":\"publish_error\"}";

        setupParameterConversions();
        when(updateTransactionUseCase.execute(any(UpdateTransactionCommand.class)))
                .thenReturn(Uni.createFrom().item(new UpdateTransactionUseCase.Result.PublishError(transaction)));
        when(updateTransactionResponseMapper.toPublishErrorDto(any(UpdateTransactionUseCase.Result.PublishError.class)))
                .thenReturn(publishErrorDto);
        when(objectMapper.writeValueAsString(publishErrorDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.updateTransaction(
                transactionId.toString(), "AAPL", "SELL", "5", "160.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-20",
                null, "NYSE", "USA"
        );

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("Should throw ToolCallException when UUID format is invalid for update")
    void testUpdateTransactionWithInvalidUUID() {
        // Given
        String invalidUuid = "not-a-uuid";

        setupParameterConversions();

        // When
        Uni<String> result = mcpServer.updateTransaction(
                invalidUuid, "AAPL", "SELL", "5", "160.00", "0.00",
                false, "1.0", "USD", "USD", "2024-01-20",
                null, "NYSE", "USA"
        );

        // Then
        result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(ToolCallException.class);
    }

    // ============ DELETE TRANSACTION TESTS ============

    @Test
    @DisplayName("Should delete transaction successfully and return JSON response")
    void testDeleteTransactionSuccess() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        DeleteTransactionResponseDto.Success successDto = new DeleteTransactionResponseDto.Success("Transaction deleted");
        String expectedJson = "{\"status\":\"success\"}";

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.Success()));
        when(deleteTransactionResponseMapper.toSuccessDto(any(DeleteTransactionUseCase.Result.Success.class)))
                .thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.deleteTransaction(transactionId.toString());

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(deleteTransactionUseCase).execute(transactionId);
    }

    @Test
    @DisplayName("Should return NotFound response when deleting non-existent transaction")
    void testDeleteTransactionNotFound() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        DeleteTransactionResponseDto.NotFound notFoundDto = new DeleteTransactionResponseDto.NotFound("Transaction not found");
        String expectedJson = "{\"status\":\"not_found\"}";

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.NotFound()));
        when(deleteTransactionResponseMapper.toNotFoundDto(any(DeleteTransactionUseCase.Result.NotFound.class)))
                .thenReturn(notFoundDto);
        when(objectMapper.writeValueAsString(notFoundDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.deleteTransaction(transactionId.toString());

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(deleteTransactionUseCase).execute(transactionId);
    }

    @Test
    @DisplayName("Should handle publish error when deleting transaction")
    void testDeleteTransactionWithPublishError() throws JsonProcessingException {
        // Given
        UUID transactionId = UUID.randomUUID();
        DeleteTransactionResponseDto.PublishError publishErrorDto =
                new DeleteTransactionResponseDto.PublishError("Event publishing failed");
        String expectedJson = "{\"status\":\"publish_error\"}";

        when(deleteTransactionUseCase.execute(transactionId))
                .thenReturn(Uni.createFrom().item(new DeleteTransactionUseCase.Result.PublishError()));
        when(deleteTransactionResponseMapper.toPublishErrorDto(any(DeleteTransactionUseCase.Result.PublishError.class)))
                .thenReturn(publishErrorDto);
        when(objectMapper.writeValueAsString(publishErrorDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.deleteTransaction(transactionId.toString());

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
    }

    // ============ GET TRANSACTIONS BY TICKER TESTS ============

    @Test
    @DisplayName("Should get transactions by ticker and return JSON response")
    void testGetTransactionsByTickerSuccess() throws JsonProcessingException {
        // Given
        String ticker = "AAPL";
        List<Transaction> transactions = List.of(createTransaction());
        TransactionDto transactionDto = createTransactionDto();
        GetTransactionsByTickerResponseDto.Success successDto =
                new GetTransactionsByTickerResponseDto.Success(List.of(transactionDto));
        String expectedJson = "{\"transactions\":[]}";

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.Success(transactions)));
        when(getTransactionsByTickerResponseMapper.toSuccessDto(any(GetTransactionByTickerUseCase.Result.Success.class)))
                .thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.getTransactionsByTicker(ticker);

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(getTransactionByTickerUseCase).getByTicker(ticker);
    }

    @Test
    @DisplayName("Should return NotFound response when ticker has no transactions")
    void testGetTransactionsByTickerNotFound() throws JsonProcessingException {
        // Given
        String ticker = "NONEXISTENT";
        GetTransactionsByTickerResponseDto.NotFound notFoundDto =
                new GetTransactionsByTickerResponseDto.NotFound("No transactions found");
        String expectedJson = "{\"status\":\"not_found\"}";

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.NotFound()));
        when(getTransactionsByTickerResponseMapper.toNotFoundDto(any(GetTransactionByTickerUseCase.Result.NotFound.class)))
                .thenReturn(notFoundDto);
        when(objectMapper.writeValueAsString(notFoundDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.getTransactionsByTicker(ticker);

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("Should return Error response when repository fails for ticker search")
    void testGetTransactionsByTickerError() throws JsonProcessingException {
        // Given
        String ticker = "AAPL";
        RuntimeException exception = new RuntimeException("Database error");
        GetTransactionsByTickerResponseDto.Error errorDto =
                new GetTransactionsByTickerResponseDto.Error("Database error");
        String expectedJson = "{\"error\":\"Database error\"}";

        when(getTransactionByTickerUseCase.getByTicker(ticker))
                .thenReturn(Uni.createFrom().item(new GetTransactionByTickerUseCase.Result.Error(exception)));
        when(getTransactionsByTickerResponseMapper.toErrorDto(any(GetTransactionByTickerUseCase.Result.Error.class)))
                .thenReturn(errorDto);
        when(objectMapper.writeValueAsString(errorDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.getTransactionsByTicker(ticker);

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
    }

    // ============ SEARCH TRANSACTIONS TESTS ============

    @Test
    @DisplayName("Should search transactions with all filters and return JSON response")
    void testSearchTransactionsWithAllFilters() throws JsonProcessingException {
        // Given
        String ticker = "AAPL";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        TransactionType type = TransactionType.BUY;
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        SearchTransactionsResponseDto.Success successDto =
                new SearchTransactionsResponseDto.Success(List.of(transactionDto));
        String expectedJson = "{\"transactions\":[]}";

        when(parameterConversionService.convert("2024-01-01", "startDate")).thenReturn(startDate);
        when(parameterConversionService.convert("2024-12-31", "endDate")).thenReturn(endDate);
        when(parameterConversionService.convert("BUY", "type")).thenReturn(type);
        when(getTransactionUseCase.searchTransactions(ticker, type, startDate, endDate))
                .thenReturn(Multi.createFrom().item(transaction));
        when(searchTransactionsResponseMapper.toSuccessDto(List.of(transaction))).thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.searchTransactions(ticker, "2024-01-01", "2024-12-31", "BUY");

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(getTransactionUseCase).searchTransactions(ticker, type, startDate, endDate);
    }

    @Test
    @DisplayName("Should search transactions without filters and return all")
    void testSearchTransactionsWithNoFilters() throws JsonProcessingException {
        // Given
        Transaction transaction = createTransaction();
        TransactionDto transactionDto = createTransactionDto();
        SearchTransactionsResponseDto.Success successDto =
                new SearchTransactionsResponseDto.Success(List.of(transactionDto));
        String expectedJson = "{\"transactions\":[]}";

        when(parameterConversionService.convert(null, "startDate")).thenReturn(null);
        when(parameterConversionService.convert(null, "endDate")).thenReturn(null);
        when(parameterConversionService.convert(null, "type")).thenReturn(null);
        when(getTransactionUseCase.searchTransactions(null, null, null, null))
                .thenReturn(Multi.createFrom().item(transaction));
        when(searchTransactionsResponseMapper.toSuccessDto(List.of(transaction))).thenReturn(successDto);
        when(objectMapper.writeValueAsString(successDto)).thenReturn(expectedJson);

        // When
        Uni<String> result = mcpServer.searchTransactions(null, null, null, null);

        // Then
        String actualJson = result.subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(expectedJson, actualJson);
        verify(getTransactionUseCase).searchTransactions(null, null, null, null);
    }

    @Test
    @DisplayName("Should throw ToolCallException when date format is invalid for search")
    void testSearchTransactionsWithInvalidDateFormat() {
        // Given
        when(parameterConversionService.convert("invalid-date", "startDate"))
                .thenThrow(new ToolCallException("Invalid date format"));

        // When/Then
        assertThrows(ToolCallException.class, () ->
                mcpServer.searchTransactions("AAPL", "invalid-date", null, null)
        );
    }

    // ============ HELPER METHODS ============

    private void setupParameterConversions() {
        when(parameterConversionService.convert(any(), eq("type"))).thenReturn(TransactionType.BUY);
        when(parameterConversionService.convert(any(), eq("quantity"))).thenReturn(new BigDecimal("10"));
        when(parameterConversionService.convert(any(), eq("price"))).thenReturn(new BigDecimal("150.00"));
        when(parameterConversionService.convert(any(), eq("fees"))).thenReturn(BigDecimal.ZERO);
        when(parameterConversionService.convert(any(), eq("fractionalMultiplier"))).thenReturn(BigDecimal.ONE);
        when(parameterConversionService.convert(any(), eq("commissionCurrency"))).thenReturn(Currency.USD);
        when(parameterConversionService.convert(any(), eq("currency"))).thenReturn(Currency.USD);
        when(parameterConversionService.convert(any(), eq("date"))).thenReturn(LocalDate.of(2024, 1, 15));
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
                LocalDate.of(2024, 1, 15),
                null,
                true,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }

    private TransactionDto createTransactionDto() {
        return new TransactionDto(
                UUID.randomUUID(),
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                null,
                false,
                BigDecimal.ONE,
                Currency.USD,
                "NYSE",
                "USA"
        );
    }
}
