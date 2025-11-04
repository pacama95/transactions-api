package com.transaction.infrastructure.incoming.mcp.converter;

import com.transaction.domain.model.Currency;
import io.quarkiverse.mcp.server.ToolCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CurrencyParameterConverter Tests")
class CurrencyParameterConverterTest {

    private CurrencyParameterConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CurrencyParameterConverter();
    }

    @ParameterizedTest
    @ValueSource(strings = {"currency", "commissionCurrency", "baseCurrency", "targetCurrency", "fromCurrency", "toCurrency"})
    @DisplayName("Should match supported parameter names")
    void shouldMatchSupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertTrue(matches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ticker", "type", "quantity", "price", "notes", "other"})
    @DisplayName("Should not match unsupported parameter names")
    void shouldNotMatchUnsupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should return Currency as target type")
    void shouldReturnCurrencyAsTargetType() {
        // Given/When
        Class<?> targetType = converter.getTargetType();

        // Then
        assertEquals(Currency.class, targetType);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        // Given/When
        Object result = converter.convert(null, "currency");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return existing Currency unchanged")
    void shouldReturnExistingCurrencyUnchanged() {
        // Given
        Currency input = Currency.USD;

        // When
        Object result = converter.convert(input, "currency");

        // Then
        assertSame(input, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "CAD"})
    @DisplayName("Should convert valid string values to Currency")
    void shouldConvertValidStringValuesToCurrency(String input) {
        // Given/When
        Object result = converter.convert(input, "currency");

        // Then
        assertInstanceOf(Currency.class, result);
        assertEquals(Currency.valueOf(input), result);
    }

    @Test
    @DisplayName("Should throw ToolCallException for invalid currency value")
    void shouldThrowToolCallExceptionForInvalidCurrency() {
        // Given
        String invalidCurrency = "INVALID";

        // When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(invalidCurrency, "currency"));

        assertTrue(exception.getMessage().contains("Invalid Currency value for parameter 'currency'"));
        assertTrue(exception.getMessage().contains(invalidCurrency));
    }

    @Test
    @DisplayName("Should throw ToolCallException for unsupported types")
    void shouldThrowToolCallExceptionForUnsupportedTypes() {
        // Given
        Object unsupportedValue = new Object();

        // When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(unsupportedValue, "currency"));

        assertTrue(exception.getMessage().contains("Cannot convert Object to Currency for parameter 'currency'"));
    }
}
