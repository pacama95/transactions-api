package com.transaction.domain.port.output;

import com.transaction.domain.event.DomainEvent;
import io.smallrye.mutiny.Uni;

/**
 * Output port for publishing domain events.
 * This interface follows the hexagonal architecture pattern,
 * allowing the domain to publish events without knowing about infrastructure details.
 * 
 * @param <T> the type of domain event data
 */
public interface EventPublisher<T extends DomainEvent<?>> {

    /**
     * Publishes a domain event to the messaging infrastructure.
     * 
     * @param domainEvent the domain event to publish
     * @return Uni<Void> representing the async publication result
     */
    Uni<Void> publish(T domainEvent);
}
