package com.transaction.infrastructure.incoming.mcp.converter;

import com.transaction.domain.model.TransactionType;
import io.quarkiverse.mcp.server.ToolCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransactionTypeParameterConverter Tests")
class TransactionTypeParameterConverterTest {

    private TransactionTypeParameterConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TransactionTypeParameterConverter();
    }

    @ParameterizedTest
    @ValueSource(strings = {"type", "transactionType", "operationType", "actionType"})
    @DisplayName("Should match supported parameter names")
    void shouldMatchSupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertTrue(matches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ticker", "currency", "quantity", "price", "notes", "other"})
    @DisplayName("Should not match unsupported parameter names")
    void shouldNotMatchUnsupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should return TransactionType as target type")
    void shouldReturnTransactionTypeAsTargetType() {
        // Given/When
        Class<?> targetType = converter.getTargetType();

        // Then
        assertEquals(TransactionType.class, targetType);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        // Given/When
        Object result = converter.convert(null, "type");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return existing TransactionType unchanged")
    void shouldReturnExistingTransactionTypeUnchanged() {
        // Given
        TransactionType input = TransactionType.BUY;

        // When
        Object result = converter.convert(input, "type");

        // Then
        assertSame(input, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BUY", "SELL", "DIVIDEND"})
    @DisplayName("Should convert valid string values to TransactionType")
    void shouldConvertValidStringValues(String input) {
        // Given/When
        Object result = converter.convert(input, "type");

        // Then
        assertInstanceOf(TransactionType.class, result);
        assertEquals(TransactionType.valueOf(input), result);
    }

    @Test
    @DisplayName("Should throw ToolCallException for invalid transaction type")
    void shouldThrowToolCallExceptionForInvalidType() {
        // Given
        String invalidType = "INVALID";

        // When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(invalidType, "type"));

        assertTrue(exception.getMessage().contains("Invalid TransactionType value for parameter 'type'"));
        assertTrue(exception.getMessage().contains(invalidType));
    }

    @Test
    @DisplayName("Should throw ToolCallException for unsupported types")
    void shouldThrowToolCallExceptionForUnsupportedTypes() {
        // Given
        Object unsupportedValue = new Object();

        // When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(unsupportedValue, "type"));

        assertTrue(exception.getMessage().contains("Cannot convert Object to TransactionType for parameter 'type'"));
    }
}
