package com.transaction.infrastructure.incoming.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.application.usecase.transaction.CreateTransactionUseCase;
import com.transaction.application.usecase.transaction.DeleteTransactionUseCase;
import com.transaction.application.usecase.transaction.GetTransactionUseCase;
import com.transaction.application.usecase.transaction.UpdateTransactionUseCase;
import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.incoming.mcp.converter.ParameterConversionService;
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
    ParameterConversionService parameterConversionService;

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
            @ToolArg(description = "Transaction notes", required = false) String notes) {

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
                    convertedCommissionCurrency
            );

            return createTransactionUseCase.execute(command)
                    .map(result -> {
                        try {
                            return objectMapper.writeValueAsString(result);
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
                        return objectMapper.writeValueAsString(transaction);
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
            @ToolArg(description = "Transaction notes", required = false) String notes) {

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
                                        convertedCommissionCurrency);
                            }
                    )
                    .flatMap(updateTransactionCommand ->
                            updateTransactionUseCase.execute(updateTransactionCommand))
                    .map(result -> {
                        try {
                            return objectMapper.writeValueAsString(result);
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
                        return objectMapper.writeValueAsString(result);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing result", e);
                    }
                })
                .onFailure().invoke(e -> Log.error("Error deleting transaction with ID %s".formatted(transactionId), e))
                .onFailure().transform(throwable -> new ToolCallException("Error deleting transaction with ID %s".formatted(transactionId)));
    }

    @Tool(description = "Get all transactions for a specific ticker.")
    public Uni<String> getTransactionsByTicker(@ToolArg(description = "Stock ticker symbol") String ticker) {
        return getTransactionUseCase.getByTicker(ticker)
                .collect().asList()
                .map(transactions -> {
                    try {
                        return objectMapper.writeValueAsString(transactions);
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
            @ToolArg(description = "Transaction type", required = false) Object type) {

        try {
            LocalDate convertedStartDate = (LocalDate) parameterConversionService.convert(startDate, "startDate");
            LocalDate convertedEndDate = (LocalDate) parameterConversionService.convert(endDate, "endDate");
            TransactionType convertedType = (TransactionType) parameterConversionService.convert(type, "type");

            return getTransactionUseCase.searchTransactions(ticker, convertedType, convertedStartDate, convertedEndDate)
                    .collect().asList()
                    .map(transactions -> {
                        try {
                            return objectMapper.writeValueAsString(transactions);
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