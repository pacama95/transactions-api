package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.DeleteTransactionResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public abstract class DeleteTransactionResponseMapper {

    public DeleteTransactionResponseDto.Success toSuccessDto(DeleteTransactionUseCase.Result.Success success) {
        return new DeleteTransactionResponseDto.Success(success.id());
    }

    public DeleteTransactionResponseDto.NotFound toNotFoundDto(DeleteTransactionUseCase.Result.NotFound notFound) {
        return new DeleteTransactionResponseDto.NotFound(notFound.id());
    }

    public DeleteTransactionResponseDto.PublishError toPublishErrorDto(DeleteTransactionUseCase.Result.PublishError publishError) {
        String errorMessage = publishError.throwable() != null 
                ? publishError.throwable().getMessage() 
                : "Failed to publish domain event";
        
        return new DeleteTransactionResponseDto.PublishError(
                publishError.id(),
                errorMessage
        );
    }

    public DeleteTransactionResponseDto.Error toErrorDto(DeleteTransactionUseCase.Result.Error error) {
        String errorMessage = formatErrorMessage(error.error().code(), error.throwable());
        return new DeleteTransactionResponseDto.Error(errorMessage);
    }

    private String formatErrorMessage(String errorCode, Throwable throwable) {
        StringBuilder message = new StringBuilder();
        if (errorCode != null && !errorCode.isEmpty()) {
            message.append("Error code: ").append(errorCode);
        }
        if (throwable != null && throwable.getMessage() != null) {
            if (!message.isEmpty()) {
                message.append(" - ");
            }
            message.append(throwable.getMessage());
        }
        return !message.isEmpty() ? message.toString() : "An error occurred";
    }
}
