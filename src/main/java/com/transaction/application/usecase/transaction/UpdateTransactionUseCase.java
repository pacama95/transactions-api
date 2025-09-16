package com.transaction.application.usecase.transaction;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.port.output.TransactionRepository;
import com.transaction.util.StringUtils;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UpdateTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @WithTransaction
    public Uni<Transaction> execute(UpdateTransactionCommand updateTransactionCommand) {
        return transactionRepository.findById(updateTransactionCommand.transactionId())
                .onItem()
                .ifNotNull().transformToUni(transaction ->
                        updateAndPersistTransaction(transaction, updateTransactionCommand)
                                .invoke(Transaction::popEvents)) // TODO: Popping events, in the future we should publish this to a queue
                .onItem()
                .ifNull().failWith(() -> new ServiceException(Errors.UpdateTransaction.NOT_FOUND));
    }

    private Uni<Transaction> updateAndPersistTransaction(Transaction current, UpdateTransactionCommand updateTransactionCommand) {
        return Uni.createFrom().item(() -> {
                    current.update(
                            StringUtils.hasMeaningfulContent(updateTransactionCommand.ticker())
                                    ? updateTransactionCommand.ticker() : null,
                            updateTransactionCommand.transactionType(),
                            updateTransactionCommand.quantity(),
                            updateTransactionCommand.price(),
                            updateTransactionCommand.fees(),
                            updateTransactionCommand.currency(),
                            updateTransactionCommand.transactionDate(),
                            StringUtils.hasMeaningfulContent(updateTransactionCommand.notes())
                                    ? updateTransactionCommand.notes() : null,
                            updateTransactionCommand.isFractional(),
                            updateTransactionCommand.fractionalMultiplier(),
                            updateTransactionCommand.commissionCurrency()
                    );
                    return current;
                })
                .flatMap(transactionUpdated -> transactionRepository.update(transactionUpdated));
    }
} 