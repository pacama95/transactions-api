package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public sealed interface UpdateTransactionResponseDto {
    
    @RegisterForReflection
    record Success(TransactionDto transaction) implements UpdateTransactionResponseDto {
    }
    
    @RegisterForReflection
    record NotFound() implements UpdateTransactionResponseDto {
    }
    
    @RegisterForReflection
    record PublishError(TransactionDto transaction, String errorMessage) implements UpdateTransactionResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements UpdateTransactionResponseDto {
    }
}
