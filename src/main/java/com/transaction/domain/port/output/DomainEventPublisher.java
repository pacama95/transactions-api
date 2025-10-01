package com.transaction.domain.port.output;

import com.transaction.domain.event.DomainEvent;
import io.smallrye.mutiny.Uni;

public interface DomainEventPublisher {

    Uni<Void> publish(DomainEvent<?> domainEvent);
}
