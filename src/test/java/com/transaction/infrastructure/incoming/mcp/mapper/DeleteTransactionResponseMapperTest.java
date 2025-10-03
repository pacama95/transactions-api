package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.DeleteTransactionResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeleteTransactionResponseMapperTest {

    @InjectMocks
    private DeleteTransactionResponseMapperImpl mapper;

    @Test
    void toSuccessDto_mapsIdCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        DeleteTransactionUseCase.Result.Success success = new DeleteTransactionUseCase.Result.Success(id);

        // When
        DeleteTransactionResponseDto.Success result = mapper.toSuccessDto(success);

        // Then
        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void toNotFoundDto_mapsIdCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        DeleteTransactionUseCase.Result.NotFound notFound = new DeleteTransactionUseCase.Result.NotFound(id);

        // When
        DeleteTransactionResponseDto.NotFound result = mapper.toNotFoundDto(notFound);

        // Then
        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void toPublishErrorDto_mapsIdAndErrorMessage() {
        // Given
        UUID id = UUID.randomUUID();
        Throwable throwable = new RuntimeException("Failed to publish delete event");
        DeleteTransactionUseCase.Result.PublishError publishError = 
                new DeleteTransactionUseCase.Result.PublishError(id, throwable);

        // When
        DeleteTransactionResponseDto.PublishError result = mapper.toPublishErrorDto(publishError);

        // Then
        assertNotNull(result);
        assertEquals(id, result.id());
        assertNotNull(result.errorMessage());
        assertEquals("Failed to publish delete event", result.errorMessage());
    }

    @Test
    void toErrorDto_formatsErrorMessageCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        com.transaction.domain.exception.Error error = new com.transaction.domain.exception.Error("DELETE_FAILED");
        Throwable throwable = new RuntimeException("Transaction is locked");
        DeleteTransactionUseCase.Result.Error errorResult = 
                new DeleteTransactionUseCase.Result.Error(error, id, throwable);

        // When
        DeleteTransactionResponseDto.Error result = mapper.toErrorDto(errorResult);

        // Then
        assertNotNull(result);
        assertNotNull(result.error());
        assertTrue(result.error().contains("DELETE_FAILED"));
        assertTrue(result.error().contains("Transaction is locked"));
    }
}

