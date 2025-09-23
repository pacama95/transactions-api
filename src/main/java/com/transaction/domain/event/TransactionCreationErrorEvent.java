package com.transaction.domain.event;

import com.transaction.domain.model.TransactionError;

public class TransactionCreationErrorEvent extends DomainEvent<TransactionError> {

    public TransactionCreationErrorEvent(TransactionError transactionError) {
        super(transactionError);
    }
}
