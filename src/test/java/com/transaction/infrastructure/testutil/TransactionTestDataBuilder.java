package com.transaction.infrastructure.testutil;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import com.transaction.infrastructure.incoming.rest.dto.CreateTransactionRequest;
import com.transaction.infrastructure.incoming.rest.dto.UpdateTransactionRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test data builder for creating transaction test data.
 * Provides convenient methods to create valid test data with sensible defaults.
 */
public class TransactionTestDataBuilder {

    // Default values for transaction creation
    private static final String DEFAULT_TICKER = "AAPL";
    private static final TransactionType DEFAULT_TYPE = TransactionType.BUY;
    private static final BigDecimal DEFAULT_QUANTITY = new BigDecimal("100.000000");
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("150.2500");
    private static final BigDecimal DEFAULT_FEES = new BigDecimal("9.9900");
    private static final Currency DEFAULT_CURRENCY = Currency.USD;
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2024, 1, 15);
    private static final String DEFAULT_NOTES = "Test transaction";
    private static final Boolean DEFAULT_IS_FRACTIONAL = false;
    private static final BigDecimal DEFAULT_FRACTIONAL_MULTIPLIER = BigDecimal.ONE;
    private static final Currency DEFAULT_COMMISSION_CURRENCY = Currency.USD;
    private static final String DEFAULT_EXCHANGE = "NASDAQ";
    private static final String DEFAULT_COUNTRY = "US";

    /**
     * Create a default CreateTransactionRequest with sensible test values
     */
    public static CreateTransactionRequest defaultCreateRequest() {
        return new CreateTransactionRequest(
                DEFAULT_TICKER,
                DEFAULT_TYPE,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest with custom ticker
     */
    public static CreateTransactionRequest createRequestWithTicker(String ticker) {
        return new CreateTransactionRequest(
                ticker,
                DEFAULT_TYPE,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest for a BUY transaction
     */
    public static CreateTransactionRequest buyTransactionRequest(
            String ticker,
            BigDecimal quantity,
            BigDecimal price) {
        return new CreateTransactionRequest(
                ticker,
                TransactionType.BUY,
                quantity,
                price,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest for a SELL transaction
     */
    public static CreateTransactionRequest sellTransactionRequest(
            String ticker,
            BigDecimal quantity,
            BigDecimal price) {
        return new CreateTransactionRequest(
                ticker,
                TransactionType.SELL,
                quantity,
                price,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest for a DIVIDEND transaction
     */
    public static CreateTransactionRequest dividendTransactionRequest(
            String ticker,
            BigDecimal amount) {
        return new CreateTransactionRequest(
                ticker,
                TransactionType.DIVIDEND,
                BigDecimal.ONE,
                amount,
                BigDecimal.ZERO,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                "Dividend payment",
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest for a fractional share transaction
     */
    public static CreateTransactionRequest fractionalShareRequest(
            String ticker,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal multiplier) {
        return new CreateTransactionRequest(
                ticker,
                TransactionType.BUY,
                quantity,
                price,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                "Fractional share purchase",
                true,
                multiplier,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest with custom date
     */
    public static CreateTransactionRequest createRequestWithDate(LocalDate date) {
        return new CreateTransactionRequest(
                DEFAULT_TICKER,
                DEFAULT_TYPE,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                date,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create a CreateTransactionRequest with custom exchange and country
     */
    public static CreateTransactionRequest createRequestWithExchange(
            String ticker,
            String exchange,
            String country) {
        return new CreateTransactionRequest(
                ticker,
                DEFAULT_TYPE,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                exchange,
                country
        );
    }

    /**
     * Create an invalid CreateTransactionRequest (missing required fields)
     */
    public static CreateTransactionRequest invalidCreateRequest() {
        return new CreateTransactionRequest(
                null,  // Invalid: ticker is required
                null,  // Invalid: type is required
                null,  // Invalid: quantity is required
                null,  // Invalid: price is required
                null,
                null,  // Invalid: currency is required
                null,  // Invalid: date is required
                null,
                null,
                null,
                null,
                null,  // Invalid: exchange is required
                null   // Invalid: country is required
        );
    }

    /**
     * Create a CreateTransactionRequest with invalid quantity (negative)
     */
    public static CreateTransactionRequest negativeQuantityRequest() {
        return new CreateTransactionRequest(
                DEFAULT_TICKER,
                DEFAULT_TYPE,
                new BigDecimal("-10.0"),  // Invalid: negative quantity
                DEFAULT_PRICE,
                DEFAULT_FEES,
                DEFAULT_CURRENCY,
                DEFAULT_DATE,
                DEFAULT_NOTES,
                DEFAULT_IS_FRACTIONAL,
                DEFAULT_FRACTIONAL_MULTIPLIER,
                DEFAULT_COMMISSION_CURRENCY,
                DEFAULT_EXCHANGE,
                DEFAULT_COUNTRY
        );
    }

    /**
     * Create an UpdateTransactionRequest with default values
     */
    public static UpdateTransactionRequest defaultUpdateRequest() {
        return new UpdateTransactionRequest(
                "GOOGL",  // Changed ticker
                TransactionType.SELL,  // Changed type
                new BigDecimal("50.0"),  // Changed quantity
                new BigDecimal("175.50"),  // Changed price
                new BigDecimal("12.50"),  // Changed fees
                Currency.EUR,  // Changed currency
                LocalDate.of(2024, 2, 20),  // Changed date
                "Updated transaction",  // Changed notes
                false,
                BigDecimal.ONE,
                Currency.EUR,
                "NYSE",  // Changed exchange
                "USA"  // Changed country
        );
    }

    /**
     * Create an UpdateTransactionRequest with partial updates
     */
    public static UpdateTransactionRequest partialUpdateRequest(String newTicker, BigDecimal newPrice) {
        return new UpdateTransactionRequest(
                newTicker,
                null,  // Keep original
                null,  // Keep original
                newPrice,
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null,  // Keep original
                null   // Keep original
        );
    }

    /**
     * Create a JSON string representation of a CreateTransactionRequest
     */
    public static String toJson(CreateTransactionRequest request) {
        return String.format("""
                {
                    "ticker": "%s",
                    "transactionType": "%s",
                    "quantity": %s,
                    "price": %s,
                    "fees": %s,
                    "currency": "%s",
                    "transactionDate": "%s",
                    "notes": "%s",
                    "isFractional": %s,
                    "fractionalMultiplier": %s,
                    "commissionCurrency": "%s",
                    "exchange": "%s",
                    "country": "%s"
                }
                """,
                request.ticker(),
                request.transactionType(),
                request.quantity(),
                request.price(),
                request.fees(),
                request.currency(),
                request.transactionDate(),
                request.notes(),
                request.isFractional(),
                request.fractionalMultiplier(),
                request.commissionCurrency(),
                request.exchange(),
                request.country()
        );
    }

    /**
     * Create a JSON string representation of an UpdateTransactionRequest
     */
    public static String toJson(UpdateTransactionRequest request) {
        return String.format("""
                {
                    "ticker": "%s",
                    "transactionType": "%s",
                    "quantity": %s,
                    "price": %s,
                    "fees": %s,
                    "currency": "%s",
                    "transactionDate": "%s",
                    "notes": "%s",
                    "isFractional": %s,
                    "fractionalMultiplier": %s,
                    "commissionCurrency": "%s",
                    "exchange": "%s",
                    "country": "%s"
                }
                """,
                request.ticker(),
                request.transactionType(),
                request.quantity(),
                request.price(),
                request.fees(),
                request.currency(),
                request.transactionDate(),
                request.notes(),
                request.isFractional(),
                request.fractionalMultiplier(),
                request.commissionCurrency(),
                request.exchange(),
                request.country()
        );
    }
}
