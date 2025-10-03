package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.domain.port.input.GetTransactionByTickerUseCase;
import com.transaction.infrastructure.incoming.mcp.dto.GetTransactionsByTickerResponseDto;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi", uses = TransactionMapper.class)
public interface GetTransactionsByTickerResponseMapper {

    List<TransactionDto> toDtoList(List<Transaction> transactions);

    default GetTransactionsByTickerResponseDto.Success toSuccessDto(GetTransactionByTickerUseCase.Result.Success success) {
        return new GetTransactionsByTickerResponseDto.Success(toDtoList(success.transactions()));
    }

    default GetTransactionsByTickerResponseDto.NotFound toNotFoundDto(GetTransactionByTickerUseCase.Result.NotFound notFound) {
        return new GetTransactionsByTickerResponseDto.NotFound();
    }

    default GetTransactionsByTickerResponseDto.Error toErrorDto(GetTransactionByTickerUseCase.Result.Error error) {
        String errorMessage = error.throwable() != null 
                ? error.throwable().getMessage() 
                : "An error occurred while fetching transactions";
        return new GetTransactionsByTickerResponseDto.Error(errorMessage);
    }
}

