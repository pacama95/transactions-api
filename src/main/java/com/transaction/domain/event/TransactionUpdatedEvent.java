package com.transaction.domain.event;

import com.transaction.domain.model.Transaction;
public class TransactionUpdatedEvent extends DomainEvent<Transaction> {
    public TransactionUpdatedEvent(Transaction transaction) {
        super(transaction);
    }
}
