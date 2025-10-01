package com.transaction.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public abstract class DomainEvent<T> {
    private final UUID eventId;
    private final Instant occurredAt;
    private final T data;

    protected DomainEvent(T data) {
        this.data = data;
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }
}
