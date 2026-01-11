package com.transaction.infrastructure.incoming.rest.dto;

import com.transaction.domain.model.Currency;
import com.transaction.domain.model.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CreateTransactionRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCreateTransactionRequest() {
        // Given
        CreateTransactionRequest request = createValidRequest();

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNullFieldTestCases")
    void testRequiredFieldsCannotBeNull(String testName, String fieldName, CreateTransactionRequest request) {
        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), testName + " should have violations");
        assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName)),
                testName + " should have violation for field: " + fieldName);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideStringLengthTestCases")
    void testStringFieldLengthConstraints(String testName, String fieldName, String invalidValue, String expectedMessage) {
        // Given
        CreateTransactionRequest request = createRequestWithField(fieldName, invalidValue);

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), testName + " should have violations");
        assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName) &&
                                v.getMessage().contains(expectedMessage)),
                testName + " should have violation with message containing: " + expectedMessage);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNumericConstraintTestCases")
    void testNumericFieldConstraints(String testName, String fieldName, BigDecimal invalidValue, String expectedMessage) {
        // Given
        CreateTransactionRequest request = createRequestWithNumericField(fieldName, invalidValue);

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), testName + " should have violations");
        assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals(fieldName) &&
                                v.getMessage().contains(expectedMessage)),
                testName + " should have violation with message containing: " + expectedMessage);
    }

    @Test
    void testTransactionDateCannotBeInFuture() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.now().plusDays(1), // Future date
                null,
                false,
                BigDecimal.ONE,
                null,
                "NYSE",
                "USA",
                "Apple Inc."
        );

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("transactionDate") &&
                        v.getMessage().contains("cannot be in the future")));
    }

    @Test
    void testNotesCanExceedMaxLength() {
        // Given
        String longNotes = "N".repeat(501);
        CreateTransactionRequest request = new CreateTransactionRequest(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                longNotes,
                false,
                BigDecimal.ONE,
                null,
                "NYSE",
                "USA",
                "Apple Inc."
        );

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("notes") &&
                        v.getMessage().contains("cannot exceed 500")));
    }

    @Test
    void testCompanyNameWithSpecialCharacters() {
        // Given
        CreateTransactionRequest request = new CreateTransactionRequest(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                null,
                false,
                BigDecimal.ONE,
                null,
                "NYSE",
                "USA",
                "Apple Inc. & Co., Ltd."
        );

        // When
        Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Company name with special characters should be valid");
    }

    // Helper methods
    private static CreateTransactionRequest createValidRequest() {
        return new CreateTransactionRequest(
                "AAPL",
                TransactionType.BUY,
                new BigDecimal("10"),
                new BigDecimal("150.50"),
                BigDecimal.ZERO,
                Currency.USD,
                LocalDate.of(2024, 1, 15),
                "Test notes",
                false,
                BigDecimal.ONE,
                null,
                "NYSE",
                "USA",
                "Apple Inc."
        );
    }

    private static CreateTransactionRequest createRequestWithField(String fieldName, String value) {
        return switch (fieldName) {
            case "ticker" -> new CreateTransactionRequest(
                    value, TransactionType.BUY, new BigDecimal("10"), new BigDecimal("150.50"),
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
            );
            case "exchange" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, new BigDecimal("10"), new BigDecimal("150.50"),
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, value, "USA", "Apple Inc."
            );
            case "country" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, new BigDecimal("10"), new BigDecimal("150.50"),
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", value, "Apple Inc."
            );
            case "companyName" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, new BigDecimal("10"), new BigDecimal("150.50"),
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", "USA", value
            );
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    private static CreateTransactionRequest createRequestWithNumericField(String fieldName, BigDecimal value) {
        return switch (fieldName) {
            case "quantity" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, value, new BigDecimal("150.50"),
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
            );
            case "price" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, new BigDecimal("10"), value,
                    BigDecimal.ZERO, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
            );
            case "fees" -> new CreateTransactionRequest(
                    "AAPL", TransactionType.BUY, new BigDecimal("10"), new BigDecimal("150.50"),
                    value, Currency.USD, LocalDate.of(2024, 1, 15), null,
                    false, BigDecimal.ONE, null, "NYSE", "USA", "Apple Inc."
            );
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    // Test data providers
    static Stream<Arguments> provideNullFieldTestCases() {
        return Stream.of(
                Arguments.of("Ticker cannot be null", "ticker",
                        new CreateTransactionRequest(null, TransactionType.BUY, new BigDecimal("10"),
                                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Transaction type cannot be null", "transactionType",
                        new CreateTransactionRequest("AAPL", null, new BigDecimal("10"),
                                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Quantity cannot be null", "quantity",
                        new CreateTransactionRequest("AAPL", TransactionType.BUY, null,
                                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Price cannot be null", "price",
                        new CreateTransactionRequest("AAPL", TransactionType.BUY, new BigDecimal("10"),
                                null, BigDecimal.ZERO, Currency.USD,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Currency cannot be null", "currency",
                        new CreateTransactionRequest("AAPL", TransactionType.BUY, new BigDecimal("10"),
                                new BigDecimal("150.50"), BigDecimal.ZERO, null,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Transaction date cannot be null", "transactionDate",
                        new CreateTransactionRequest("AAPL", TransactionType.BUY, new BigDecimal("10"),
                                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                                null, null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", "Apple Inc.")),
                Arguments.of("Company name cannot be null", "companyName",
                        new CreateTransactionRequest("AAPL", TransactionType.BUY, new BigDecimal("10"),
                                new BigDecimal("150.50"), BigDecimal.ZERO, Currency.USD,
                                LocalDate.of(2024, 1, 15), null, false, BigDecimal.ONE,
                                null, "NYSE", "USA", null))
        );
    }

    static Stream<Arguments> provideStringLengthTestCases() {
        return Stream.of(
                Arguments.of("Ticker too short", "ticker", "", "between 1 and 10"),
                Arguments.of("Ticker too long", "ticker", "A".repeat(11), "between 1 and 10"),
                Arguments.of("Exchange too short", "exchange", "", "between 1 and 20"),
                Arguments.of("Exchange too long", "exchange", "E".repeat(21), "between 1 and 20"),
                Arguments.of("Country too short", "country", "A", "between 2 and 50"),
                Arguments.of("Country too long", "country", "C".repeat(51), "between 2 and 50"),
                Arguments.of("Company name too short", "companyName", "", "between 1 and 255"),
                Arguments.of("Company name too long", "companyName", "C".repeat(256), "between 1 and 255")
        );
    }

    static Stream<Arguments> provideNumericConstraintTestCases() {
        return Stream.of(
                Arguments.of("Quantity must be positive", "quantity", new BigDecimal("0"), "must be positive"),
                Arguments.of("Quantity too small", "quantity", new BigDecimal("0.00001"), "must be positive"),
                Arguments.of("Price must be positive", "price", new BigDecimal("0"), "must be positive"),
                Arguments.of("Price too small", "price", new BigDecimal("0.001"), "must be positive"),
                Arguments.of("Fees cannot be negative", "fees", new BigDecimal("-1"), "cannot be negative")
        );
    }
}
