package com.transaction.infrastructure.incoming.mcp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

@RegisterForReflection
public sealed interface DeleteTransactionResponseDto {
    
    @RegisterForReflection
    record Success(UUID id) implements DeleteTransactionResponseDto {
    }
    
    @RegisterForReflection
    record NotFound(UUID id) implements DeleteTransactionResponseDto {
    }
    
    @RegisterForReflection
    record PublishError(UUID id, String errorMessage) implements DeleteTransactionResponseDto {
    }
    
    @RegisterForReflection
    record Error(String error) implements DeleteTransactionResponseDto {
    }
}
