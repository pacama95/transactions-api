package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public sealed interface CreateTransactionResponseDto {
    
    @RegisterForReflection
    record Success(TransactionDto transaction) implements CreateTransactionResponseDto {
    }
    
    @RegisterForReflection
    record PublishError(TransactionDto transaction, String errorMessage) implements CreateTransactionResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements CreateTransactionResponseDto {
    }
}
