package com.transaction.infrastructure.outgoing.messaging.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

/**
 * Payload data for transaction deleted events.
 */
@RegisterForReflection
public record TransactionDeletedData(
        UUID id
) {
}
