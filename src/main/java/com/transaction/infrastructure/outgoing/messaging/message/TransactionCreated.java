package com.transaction.infrastructure.outgoing.messaging.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.UUID;

/**
 * Type alias for transaction created messages using the base Message wrapper.
 * This provides a convenient way to work with transaction created events
 * while maintaining consistency with the common message structure.
 */
@RegisterForReflection
public final class TransactionCreated {

    /**
     * Creates a new transaction created message.
     */
    public static Message<TransactionCreatedData> create(
            UUID eventId,
            Instant occurredAt,
            Instant messageCreatedAt,
            TransactionCreatedData payload) {
        return new Message<>(eventId, occurredAt, messageCreatedAt, payload);
    }

    // Private constructor to prevent instantiation
    private TransactionCreated() {
        throw new UnsupportedOperationException("This is a utility class");
    }
}
