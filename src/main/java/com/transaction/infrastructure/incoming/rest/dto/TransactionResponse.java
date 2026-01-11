package com.transaction.infrastructure.incoming.rest.dto;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@Schema(description = "Transaction response with all transaction details")
public record TransactionResponse(
        @Schema(description = "Unique transaction identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Stock ticker symbol", example = "AAPL")
        String ticker,
        @Schema(description = "Type of transaction")
        TransactionType transactionType,
        @Schema(description = "Number of shares", example = "100.00")
        BigDecimal quantity,
        @Schema(description = "Price per share", example = "150.25")
        BigDecimal price,
        @Schema(description = "Transaction fees", example = "9.99")
        BigDecimal fees,
        @Schema(description = "Transaction currency")
        Currency currency,
        @Schema(description = "Date of the transaction", example = "2023-10-15")
        LocalDate transactionDate,
        @Schema(description = "Optional notes about the transaction")
        String notes,
        @Schema(description = "Whether the transaction is active")
        Boolean isActive,
        @Schema(description = "Total transaction value", example = "15025.00")
        BigDecimal totalValue,
        @Schema(description = "Total transaction cost including fees", example = "15034.99")
        BigDecimal totalCost,
        @Schema(description = "Whether this is a fractional share transaction")
        Boolean isFractional,
        @Schema(description = "Multiplier for fractional shares")
        BigDecimal fractionalMultiplier,
        @Schema(description = "Currency for commission fees")
        Currency commissionCurrency,
        @Schema(description = "Stock exchange", example = "NYSE")
        String exchange,
        @Schema(description = "Country of the stock", example = "USA")
        String country,
        @Schema(description = "Company legal name", example = "Apple Inc.")
        String companyName
) {

    public TransactionResponse {
        if (isFractional == null) {
            isFractional = false;
        }
        if (fractionalMultiplier == null) {
            fractionalMultiplier = BigDecimal.ONE;
        }
    }
} 