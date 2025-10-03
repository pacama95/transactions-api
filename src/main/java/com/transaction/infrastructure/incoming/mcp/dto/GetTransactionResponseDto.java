package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public sealed interface GetTransactionResponseDto {
    
    @RegisterForReflection
    record Success(TransactionDto transaction) implements GetTransactionResponseDto {
    }
    
    @RegisterForReflection
    record NotFound() implements GetTransactionResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements GetTransactionResponseDto {
    }
}
