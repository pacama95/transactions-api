package com.transaction.domain.event;

import com.transaction.domain.model.TransactionUpdateData;

public class TransactionUpdatedEvent extends DomainEvent<TransactionUpdateData> {
    public TransactionUpdatedEvent(TransactionUpdateData data) {
        super(data);
    }
}
