package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public sealed interface SearchTransactionsResponseDto {
    
    @RegisterForReflection
    record Success(List<TransactionDto> transactions) implements SearchTransactionsResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements SearchTransactionsResponseDto {
    }
}
