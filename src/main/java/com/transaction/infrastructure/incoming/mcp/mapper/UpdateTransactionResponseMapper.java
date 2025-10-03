package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.UpdateTransactionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = TransactionMapper.class)
public interface UpdateTransactionResponseMapper {

    @Mapping(target = "transaction", source = "transaction")
    UpdateTransactionResponseDto.Success toSuccessDto(UpdateTransactionUseCase.Result.Success success);

    default UpdateTransactionResponseDto.NotFound toNotFoundDto(UpdateTransactionUseCase.Result.NotFound notFound) {
        return new UpdateTransactionResponseDto.NotFound();
    }

    default UpdateTransactionResponseDto.PublishError toPublishErrorDto(UpdateTransactionUseCase.Result.PublishError publishError) {
        String errorMessage = publishError.throwable() != null 
                ? publishError.throwable().getMessage() 
                : "Failed to publish domain event";
        
        return new UpdateTransactionResponseDto.PublishError(
                toSuccessDto(new UpdateTransactionUseCase.Result.Success(publishError.transaction())).transaction(),
                errorMessage
        );
    }

    default UpdateTransactionResponseDto.Error toErrorDto(UpdateTransactionUseCase.Result.Error error) {
        String errorMessage = formatErrorMessage(error.error().code(), error.throwable());
        return new UpdateTransactionResponseDto.Error(errorMessage);
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
