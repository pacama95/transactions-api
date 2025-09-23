package com.transaction.domain.event;

import com.transaction.domain.model.Transaction;

public class TransactionDeletedEvent extends DomainEvent<Transaction> {
    public TransactionDeletedEvent(Transaction transaction) {
        super(transaction);
    }
}


