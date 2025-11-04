package com.transaction.infrastructure.incoming.mcp.converter;

import io.quarkiverse.mcp.server.ToolCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalDateParameterConverter Tests")
class LocalDateParameterConverterTest {

    private LocalDateParameterConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateParameterConverter();
    }

    @ParameterizedTest
    @ValueSource(strings = {"date", "startDate", "endDate", "transactionDate", "createdDate", "updatedDate", "fromDate", "toDate"})
    @DisplayName("Should match supported parameter names")
    void shouldMatchSupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertTrue(matches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ticker", "type", "currency", "price", "notes", "other"})
    @DisplayName("Should not match unsupported parameter names")
    void shouldNotMatchUnsupportedParameterNames(String parameterName) {
        // Given/When
        boolean matches = converter.matches(parameterName);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should return LocalDate as target type")
    void shouldReturnLocalDateAsTargetType() {
        // Given/When
        Class<?> targetType = converter.getTargetType();

        // Then
        assertEquals(LocalDate.class, targetType);
    }

    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
        // Given/When
        Object result = converter.convert(null, "date");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return existing LocalDate unchanged")
    void shouldReturnExistingLocalDateUnchanged() {
        // Given
        LocalDate input = LocalDate.of(2024, 1, 15);

        // When
        Object result = converter.convert(input, "date");

        // Then
        assertSame(input, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-15", "2023-12-31", "2025-06-01", "2024-02-29"})
    @DisplayName("Should convert valid date strings to LocalDate")
    void shouldConvertValidDateStrings(String input) {
        // Given/When
        Object result = converter.convert(input, "date");

        // Then
        assertInstanceOf(LocalDate.class, result);
        assertEquals(LocalDate.parse(input), result);
    }

    @Test
    @DisplayName("Should convert TODAY keyword to current date")
    void shouldConvertTodayKeyword() {
        // Given
        String input = "TODAY";

        // When
        Object result = converter.convert(input, "date");

        // Then
        assertInstanceOf(LocalDate.class, result);
        assertEquals(LocalDate.now(), result);
    }

    @Test
    @DisplayName("Should handle trimming of string values")
    void shouldHandleTrimmingOfStringValues() {
        // Given
        String input = "  2024-01-15  ";

        // When
        Object result = converter.convert(input, "date");

        // Then
        assertInstanceOf(LocalDate.class, result);
        assertEquals(LocalDate.of(2024, 1, 15), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024/01/15", "15-01-2024", "invalid", "2024-13-01", "abc"})
    @DisplayName("Should throw ToolCallException for invalid date format")
    void shouldThrowToolCallExceptionForInvalidDateFormat(String input) {
        // Given/When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(input, "date"));

        assertTrue(exception.getMessage().contains("Invalid LocalDate value for parameter 'date'"));
        assertTrue(exception.getMessage().contains(input));
    }

    @Test
    @DisplayName("Should throw ToolCallException for unsupported types")
    void shouldThrowToolCallExceptionForUnsupportedTypes() {
        // Given
        Object unsupportedValue = new Object();

        // When/Then
        ToolCallException exception = assertThrows(ToolCallException.class,
                () -> converter.convert(unsupportedValue, "date"));

        assertTrue(exception.getMessage().contains("Cannot convert Object to LocalDate for parameter 'date'"));
    }
}
