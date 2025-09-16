package com.transaction.application.usecase.transaction;

import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.port.output.TransactionRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class DeleteTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @WithTransaction
    public Uni<Boolean> execute(UUID id) {
        return transactionRepository.findById(id)
                .onFailure().transform(throwable ->
                        new ServiceException(Errors.DeleteTransaction.PERSISTENCE_ERROR, throwable))
                .flatMap(transaction -> {
                    if (transaction == null) {
                        return Uni.createFrom().item(false);
                    }

                    return transactionRepository.deleteById(id)
                            .flatMap(deleted -> {
                                if (deleted) {
                                    return Uni.createFrom().item(true);
                                } else {
                                    return Uni.createFrom().item(false);
                                }
                            });
                });
    }
} 