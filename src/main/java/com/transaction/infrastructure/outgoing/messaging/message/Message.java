package com.transaction.infrastructure.outgoing.messaging.message;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic message wrapper containing common metadata fields for all Kafka messages.
 * 
 * @param <T> the type of the message payload
 */
@RegisterForReflection
public record Message<T>(
        UUID eventId,
        Instant occurredAt,
        Instant messageCreatedAt,
        T payload
) {
}