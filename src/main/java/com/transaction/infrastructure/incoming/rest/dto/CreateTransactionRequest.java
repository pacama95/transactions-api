package com.transaction.infrastructure.incoming.rest.dto;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@RegisterForReflection
@Schema(description = "Request to create a new transaction")
public record CreateTransactionRequest(
        @NotNull(message = "Ticker is required")
        @Size(min = 1, max = 10, message = "Ticker must be between 1 and 10 characters")
        @Schema(description = "Stock ticker symbol", example = "AAPL", required = true)
        String ticker,

        @NotNull(message = "Transaction type is required")
        @Schema(description = "Type of transaction (BUY/SELL)", required = true)
        TransactionType transactionType,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        @Schema(description = "Number of shares", example = "100.00", required = true)
        BigDecimal quantity,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        @Schema(description = "Price per share", example = "150.25", required = true)
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Fees cannot be negative")
        @Schema(description = "Transaction fees", example = "9.99", defaultValue = "0.0")
        BigDecimal fees,

        @NotNull(message = "Currency is required")
        @Schema(description = "Transaction currency", required = true)
        Currency currency,

        @NotNull(message = "Transaction date is required")
        @PastOrPresent(message = "Transaction date cannot be in the future")
        @Schema(description = "Date of the transaction", example = "2023-10-15", required = true)
        LocalDate transactionDate,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        @Schema(description = "Optional notes about the transaction", example = "Quarterly investment")
        String notes,

        @Schema(description = "Whether this is a fractional share transaction", defaultValue = "false")
        Boolean isFractional,
        @Schema(description = "Multiplier for fractional shares", defaultValue = "1.0")
        BigDecimal fractionalMultiplier,
        @Schema(description = "Currency for commission fees")
        Currency commissionCurrency,
        @NotNull(message = "Exchange is required")
        @Size(min = 1, max = 20, message = "Exchange must be between 1 and 20 characters")
        @Schema(description = "Stock exchange", example = "NYSE", required = true)
        String exchange,
        @NotNull(message = "Country is required")
        @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
        @Schema(description = "Country of the stock", example = "USA", required = true)
        String country
) {

    public CreateTransactionRequest {
        if (fees == null) {
            fees = BigDecimal.ZERO;
        }
        if (isFractional == null) {
            isFractional = false;
        }
        if (fractionalMultiplier == null) {
            fractionalMultiplier = BigDecimal.ONE;
        }
    }
} 