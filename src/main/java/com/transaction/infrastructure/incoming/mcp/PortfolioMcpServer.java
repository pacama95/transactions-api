package com.transaction.infrastructure.incoming.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.application.usecase.transaction.GetTransactionUseCase;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.converter.ParameterConversionService;
import com.transaction.infrastructure.incoming.mcp.dto.*;
import com.transaction.infrastructure.incoming.mcp.mapper.*;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Singleton
public class PortfolioMcpServer {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CreateTransactionUseCase createTransactionUseCase;

    @Inject
    GetTransactionUseCase getTransactionUseCase;

    @Inject
    UpdateTransactionUseCase updateTransactionUseCase;

    @Inject
    DeleteTransactionUseCase deleteTransactionUseCase;

    @Inject
    GetTransactionByTickerUseCase getTransactionByTickerUseCase;

    @Inject
    ParameterConversionService parameterConversionService;

    @Inject
    CreateTransactionResponseMapper createTransactionResponseMapper;

    @Inject
    UpdateTransactionResponseMapper updateTransactionResponseMapper;

    @Inject
    DeleteTransactionResponseMapper deleteTransactionResponseMapper;

    @Inject
    GetTransactionResponseMapper getTransactionResponseMapper;

    @Inject
    GetTransactionsByTickerResponseMapper getTransactionsByTickerResponseMapper;

    @Inject
    SearchTransactionsResponseMapper searchTransactionsResponseMapper;

    // ============ MCP TOOL METHODS ============

    @Tool(description = "Create a new transaction in the portfolio.")
    public Uni<String> createTransaction(
            @ToolArg(description = "Stock ticker symbol") String ticker,
            @ToolArg(description = "Transaction type (BUY, SELL, DIVIDEND)") Object type,
            @ToolArg(description = "Quantity of shares") Object quantity,
            @ToolArg(description = "Price per share") Object price,
            @ToolArg(description = "Fees paid per transaction", required = false, defaultValue = "0.00") Object fees,
            @ToolArg(description = "Determine if this is an operation on a stock fraction (for fractional offerings)", required = false, defaultValue = "false") boolean isFractional,
            @ToolArg(description = "Fraction of the real stock option represented by this fractional offered option", required = false, defaultValue = "1.0") Object fractionalMultiplier,
            @ToolArg(description = "Fees currency", required = false, defaultValue = "USD") Object commissionCurrency,
            @ToolArg(description = "Transaction currency") Object currency,
            @ToolArg(description = "Transaction date (YYYY-MM-DD)", defaultValue = "TODAY") Object date,
            @ToolArg(description = "Transaction notes", required = false) String notes,
            @ToolArg(description = "Stock exchange") String exchange,
            @ToolArg(description = "Country of the stock") String country) {

        try {
            TransactionType convertedType = (TransactionType) parameterConversionService.convert(type, "type");
            BigDecimal convertedQuantity = (BigDecimal) parameterConversionService.convert(quantity, "quantity");
            BigDecimal convertedPrice = (BigDecimal) parameterConversionService.convert(price, "price");
            BigDecimal convertedFees = (BigDecimal) parameterConversionService.convert(fees, "fees");
            BigDecimal convertedFractionalMultiplier = (BigDecimal) parameterConversionService.convert(fractionalMultiplier, "fractionalMultiplier");
            Currency convertedCommissionCurrency = (Currency) parameterConversionService.convert(commissionCurrency, "commissionCurrency");
            Currency convertedCurrency = (Currency) parameterConversionService.convert(currency, "currency");
            LocalDate convertedDate = (LocalDate) parameterConversionService.convert(date, "date");

            CreateTransactionCommand command = new CreateTransactionCommand(
                    ticker,
                    convertedType,
                    convertedQuantity,
                    convertedPrice,
                    convertedFees,
                    convertedCurrency,
                    convertedDate,
                    notes,
                    isFractional,
                    convertedFractionalMultiplier,
                    convertedCommissionCurrency,
                    exchange,
                    country
            );

            return createTransactionUseCase.execute(command)
                    .map(result -> {
                        try {
                            CreateTransactionResponseDto responseDto = switch (result) {
                                case CreateTransactionUseCase.Result.Success success ->
                                        createTransactionResponseMapper.toSuccessDto(success);
                                case CreateTransactionUseCase.Result.PublishError publishError ->
                                        createTransactionResponseMapper.toPublishErrorDto(publishError);
                                case CreateTransactionUseCase.Result.Error error ->
                                        createTransactionResponseMapper.toErrorDto(error);
                            };
                            return objectMapper.writeValueAsString(responseDto);
                        } catch (Exception e) {
                            throw new RuntimeException("Error serializing result", e);
                        }
                    }).onFailure()
                    .invoke(e -> Log.error("Error creating transaction with ticker %s".formatted(ticker), e))
                    .onFailure().transform(throwable -> new ToolCallException("Error creating transaction with ticker %s".formatted(ticker)));
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("Validation error", e);
        }
    }

    @Tool(description = "Get a transaction by its ID.")
    public Uni<String> getTransaction(@ToolArg(description = "The ID of the transaction to retrieve (UUID format)") String transactionId) {
        return Uni.createFrom().item(() -> UUID.fromString(transactionId))
                .flatMap(trxId -> getTransactionUseCase.getById(trxId))
                .map(transaction -> {
                    try {
                        GetTransactionResponseDto.Success responseDto = getTransactionResponseMapper.toSuccessDto(transaction);
                        return objectMapper.writeValueAsString(responseDto);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                })
                .onFailure().invoke(e -> Log.error("Error getting transaction with ID %s".formatted(transactionId), e))
                .onFailure().transform(throwable -> new ToolCallException("Error getting transaction with ID %s".formatted(transactionId)));
    }

    @Tool(description = "Update an existing transaction given its transaciton ID.")
    public Uni<String> updateTransaction(
            @ToolArg(description = "Transaction ID to update (UUID format)") String transactionId,
            @ToolArg(description = "Stock ticker symbol", required = false) String ticker,
            @ToolArg(description = "Transaction type (BUY, SELL, DIVIDEND)", required = false) Object type,
            @ToolArg(description = "Quantity of shares", required = false) Object quantity,
            @ToolArg(description = "Price per share", required = false) Object price,
            @ToolArg(description = "Fees paid per transaction", required = false, defaultValue = "0.00") Object fees,
            @ToolArg(description = "Determine if this is an operation on a stock fraction (for fractional offerings)", required = false, defaultValue = "false") boolean isFractional,
            @ToolArg(description = "Fraction of the real stock option represented by this fractional offered option", required = false, defaultValue = "1.0") Object fractionalMultiplier,
            @ToolArg(description = "Fees currency", required = false, defaultValue = "USD") Object commissionCurrency,
            @ToolArg(description = "Transaction currency", required = false, defaultValue = "USD") Object currency,
            @ToolArg(description = "Transaction date (YYYY-MM-DD)", required = false, defaultValue = "TODAY") Object date,
            @ToolArg(description = "Transaction notes", required = false) String notes,
            @ToolArg(description = "Stock exchange", required = false) String exchange,
            @ToolArg(description = "Country of the stock", required = false) String country) {

        try {
            return Uni.createFrom().item(() -> {
                                TransactionType convertedType = (TransactionType) parameterConversionService.convert(type, "type");
                                BigDecimal convertedQuantity = (BigDecimal) parameterConversionService.convert(quantity, "quantity");
                                BigDecimal convertedPrice = (BigDecimal) parameterConversionService.convert(price, "price");
                                BigDecimal convertedFees = (BigDecimal) parameterConversionService.convert(fees, "fees");
                                BigDecimal convertedFractionalMultiplier = (BigDecimal) parameterConversionService.convert(fractionalMultiplier, "fractionalMultiplier");
                                Currency convertedCommissionCurrency = (Currency) parameterConversionService.convert(commissionCurrency, "commissionCurrency");
                                Currency convertedCurrency = (Currency) parameterConversionService.convert(currency, "currency");
                                LocalDate convertedDate = (LocalDate) parameterConversionService.convert(date, "date");

                                return new UpdateTransactionCommand(
                                        UUID.fromString(transactionId),
                                        ticker,
                                        convertedType,
                                        convertedQuantity,
                                        convertedPrice,
                                        convertedFees,
                                        convertedCurrency,
                                        convertedDate,
                                        notes,
                                        isFractional,
                                        convertedFractionalMultiplier,
                                        convertedCommissionCurrency,
                                        exchange,
                                        country);
                            }
                    )
                    .flatMap(updateTransactionCommand -> updateTransactionUseCase.execute(updateTransactionCommand))
                    .map(result -> {
                        try {
                            UpdateTransactionResponseDto responseDto = switch (result) {
                                case UpdateTransactionUseCase.Result.Success success ->
                                        updateTransactionResponseMapper.toSuccessDto(success);
                                case UpdateTransactionUseCase.Result.PublishError publishError ->
                                        updateTransactionResponseMapper.toPublishErrorDto(publishError);
                                case UpdateTransactionUseCase.Result.NotFound notFound ->
                                        updateTransactionResponseMapper.toNotFoundDto(notFound);
                                case UpdateTransactionUseCase.Result.Error error ->
                                        updateTransactionResponseMapper.toErrorDto(error);
                            };
                            return objectMapper.writeValueAsString(responseDto);
                        } catch (Exception e) {
                            throw new RuntimeException("Error serializing result", e);
                        }
                    })
                    .onFailure().invoke(e -> Log.error("Error updating transaction with ID %s".formatted(transactionId), e))
                    .onFailure().transform(throwable -> new ToolCallException("Error updating transaction with ID %s".formatted(transactionId)));
        } catch (Exception e) {
            throw new ToolCallException("Validation error", e);
        }
    }

    @Tool(description = "Delete a transaction by ID.")
    public Uni<String> deleteTransaction(@ToolArg(description = "The ID of the transaction to delete (UUID format)") String transactionId) {
        return Uni.createFrom().item(() -> UUID.fromString(transactionId))
                .flatMap(trxId -> deleteTransactionUseCase.execute(trxId))
                .map(result -> {
                    try {
                        DeleteTransactionResponseDto responseDto = switch (result) {
                            case DeleteTransactionUseCase.Result.Success success ->
                                    deleteTransactionResponseMapper.toSuccessDto(success);
                            case DeleteTransactionUseCase.Result.NotFound notFound ->
                                    deleteTransactionResponseMapper.toNotFoundDto(notFound);
                            case DeleteTransactionUseCase.Result.PublishError publishError ->
                                    deleteTransactionResponseMapper.toPublishErrorDto(publishError);
                            case DeleteTransactionUseCase.Result.Error error ->
                                    deleteTransactionResponseMapper.toErrorDto(error);
                        };
                        return objectMapper.writeValueAsString(responseDto);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                })
                .onFailure().invoke(e -> Log.error("Error deleting transaction with ID %s".formatted(transactionId), e))
                .onFailure().transform(throwable -> new ToolCallException("Error deleting transaction with ID %s".formatted(transactionId)));
    }

    @Tool(description = "Get all transactions for a specific ticker.")
    public Uni<String> getTransactionsByTicker(
            @ToolArg(description = "Stock ticker symbol") String ticker,
            @ToolArg(description = "Maximum number of transactions to return. If not specified, returns all transactions. Returns the most recent transactions when limit is applied.", required = false) Integer limit) {
        return getTransactionByTickerUseCase.getByTicker(ticker)
                .map(result -> {
                    try {
                        GetTransactionsByTickerResponseDto responseDto = switch (result) {
                            case GetTransactionByTickerUseCase.Result.Success success -> {
                                // Apply limit if specified
                                var transactions = success.transactions();
                                if (limit != null && limit > 0 && transactions.size() > limit) {
                                    // Get the last 'limit' transactions
                                    transactions = transactions.subList(Math.max(0, transactions.size() - limit), transactions.size());
                                }
                                yield getTransactionsByTickerResponseMapper.toSuccessDto(
                                        new GetTransactionByTickerUseCase.Result.Success(transactions));
                            }
                            case GetTransactionByTickerUseCase.Result.NotFound notFound ->
                                    getTransactionsByTickerResponseMapper.toNotFoundDto(notFound);
                            case GetTransactionByTickerUseCase.Result.Error error ->
                                    getTransactionsByTickerResponseMapper.toErrorDto(error);
                        };
                        return objectMapper.writeValueAsString(responseDto);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                })
                .onFailure().invoke(e -> Log.error("Error getting transactions for ticker %s".formatted(ticker), e))
                .onFailure().transform(throwable -> new ToolCallException("Error getting transactions for ticker %s".formatted(ticker)));
    }

    @Tool(description = "Search transactions with multiple filters.")
    public Uni<String> searchTransactions(
            @ToolArg(description = "Stock ticker symbol", required = false) String ticker,
            @ToolArg(description = "Start date (YYYY-MM-DD)", required = false) Object startDate,
            @ToolArg(description = "End date (YYYY-MM-DD)", required = false) Object endDate,
            @ToolArg(description = "Transaction type", required = false) Object type,
            @ToolArg(description = "Maximum number of transactions to return. If not specified, returns all transactions. Returns the most recent transactions when limit is applied.", required = false) Integer limit) {

        try {
            LocalDate convertedStartDate = (LocalDate) parameterConversionService.convert(startDate, "startDate");
            LocalDate convertedEndDate = (LocalDate) parameterConversionService.convert(endDate, "endDate");
            TransactionType convertedType = (TransactionType) parameterConversionService.convert(type, "type");

            return getTransactionUseCase.searchTransactions(ticker, convertedType, convertedStartDate, convertedEndDate)
                    .collect().asList()
                    .map(transactions -> {
                        try {
                            // Apply limit if specified
                            var limitedTransactions = transactions;
                            if (limit != null && limit > 0 && transactions.size() > limit) {
                                // Get the last 'limit' transactions
                                limitedTransactions = transactions.subList(Math.max(0, transactions.size() - limit), transactions.size());
                            }
                            SearchTransactionsResponseDto.Success responseDto = searchTransactionsResponseMapper.toSuccessDto(limitedTransactions);
                            return objectMapper.writeValueAsString(responseDto);
                        } catch (Exception e) {
                            throw new RuntimeException("Error serializing result", e);
                        }
                    })
                    .onFailure().invoke(e -> Log.error("Error searching transactions", e))
                    .onFailure().transform(throwable -> new ToolCallException("Error searching transactions"));
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("Validation error", e);
        }
    }
} 