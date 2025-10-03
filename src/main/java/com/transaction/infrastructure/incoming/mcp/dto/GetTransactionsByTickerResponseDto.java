package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public sealed interface GetTransactionsByTickerResponseDto {
    
    @RegisterForReflection
    record Success(List<TransactionDto> transactions) implements GetTransactionsByTickerResponseDto {
    }
    
    @RegisterForReflection
    record NotFound() implements GetTransactionsByTickerResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements GetTransactionsByTickerResponseDto {
    }
}
