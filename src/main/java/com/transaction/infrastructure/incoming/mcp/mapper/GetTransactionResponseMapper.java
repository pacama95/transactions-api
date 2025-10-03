package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.incoming.mcp.dto.GetTransactionResponseDto;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi", uses = TransactionMapper.class)
public interface GetTransactionResponseMapper {

    TransactionDto toDto(Transaction transaction);

    default GetTransactionResponseDto.Success toSuccessDto(Transaction transaction) {
        return new GetTransactionResponseDto.Success(toDto(transaction));
    }

    default GetTransactionResponseDto.NotFound toNotFoundDto() {
        return new GetTransactionResponseDto.NotFound();
    }

    default GetTransactionResponseDto.Error toErrorDto(String error) {
        return new GetTransactionResponseDto.Error(error);
    }
}
