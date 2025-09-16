package com.transaction.domain.event;

import com.transaction.domain.model.Transaction;

public class TransactionCreatedEvent extends DomainEvent<Transaction> {

    public TransactionCreatedEvent(Transaction transaction) {
        super(transaction);
    }
}
