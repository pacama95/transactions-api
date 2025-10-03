package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.port.input.CreateTransactionUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.CreateTransactionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = TransactionMapper.class)
public interface CreateTransactionResponseMapper {

    @Mapping(target = "transaction", source = "transaction")
    CreateTransactionResponseDto.Success toSuccessDto(CreateTransactionUseCase.Result.Success success);

    default CreateTransactionResponseDto.PublishError toPublishErrorDto(CreateTransactionUseCase.Result.PublishError publishError) {
        String errorMessage = publishError.throwable() != null 
                ? publishError.throwable().getMessage() 
                : "Failed to publish domain event";
        
        return new CreateTransactionResponseDto.PublishError(
                toSuccessDto(new CreateTransactionUseCase.Result.Success(publishError.transaction())).transaction(),
                errorMessage
        );
    }

    default CreateTransactionResponseDto.Error toErrorDto(CreateTransactionUseCase.Result.Error error) {
        String errorMessage = formatErrorMessage(error.error().code(), error.throwable());
        return new CreateTransactionResponseDto.Error(errorMessage);
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
