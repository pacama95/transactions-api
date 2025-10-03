package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.incoming.mcp.dto.SearchTransactionsResponseDto;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi", uses = TransactionMapper.class)
public interface SearchTransactionsResponseMapper {

    List<TransactionDto> toDtoList(List<Transaction> transactions);

    default SearchTransactionsResponseDto.Success toSuccessDto(List<Transaction> transactions) {
        return new SearchTransactionsResponseDto.Success(toDtoList(transactions));
    }

    default SearchTransactionsResponseDto.Error toErrorDto(String error) {
        return new SearchTransactionsResponseDto.Error(error);
    }
}
