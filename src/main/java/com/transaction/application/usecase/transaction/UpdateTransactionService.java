package com.transaction.application.usecase.transaction;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.exception.Error;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.domain.port.output.EventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import com.transaction.util.StringUtils;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

@ApplicationScoped
public class UpdateTransactionService implements UpdateTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    @Named("redisPublisher")
    EventPublisher<DomainEvent<?>> eventPublisher;

    @Override
    @WithTransaction
    public Uni<Result> execute(UpdateTransactionCommand command) {
        return transactionRepository.findById(command.transactionId())
                .flatMap(found -> {
                    if (found == null) {
                        return Uni.createFrom().item(new Result.NotFound());
                    }
                    return updateAndPersist(found, command)
                            .map(UpdateTransactionService::success);
                })
                .onFailure().recoverWithItem(throwable -> transformToError(throwable, command))
                .flatMap(this::publishDomainEvents);
    }

    private Uni<Transaction> updateAndPersist(Transaction current, UpdateTransactionCommand command) {
        return Uni.createFrom().item(() -> {
                    current.update(
                            StringUtils.hasMeaningfulContent(command.ticker()) ? command.ticker() : null,
                            command.transactionType(),
                            command.quantity(),
                            command.price(),
                            command.fees(),
                            command.currency(),
                            command.transactionDate(),
                            StringUtils.hasMeaningfulContent(command.notes()) ? command.notes() : null,
                            command.isFractional(),
                            command.fractionalMultiplier(),
                            command.commissionCurrency(),
                            StringUtils.hasMeaningfulContent(command.exchange()) ? command.exchange() : null,
                            StringUtils.hasMeaningfulContent(command.country()) ? command.country() : null
                    );
                    return current;
                })
                .flatMap(updated -> transactionRepository.update(updated).replaceWith(updated));
    }

    private Uni<Result> publishDomainEvents(Result result) {
        if (result instanceof Result.Success success) {
            Transaction transaction = success.transaction();
            List<DomainEvent<?>> events = transaction.popEvents();
            if (events.isEmpty()) {
                return Uni.createFrom().item(result);
            }
            return Multi.createFrom().iterable(events)
                    .onItem().transformToUniAndMerge(event -> eventPublisher.publish(event))
                    .collect().asList()
                    .replaceWith(result)
                    .onFailure().recoverWithItem(throwable -> new Result.PublishError(transaction, throwable));
        }
        return Uni.createFrom().item(result);
    }

    private static Result success(Transaction transaction) {
        return new Result.Success(transaction);
    }

    private static Result transformToError(Throwable throwable, UpdateTransactionCommand command) {
        Log.error("UpdateTransactionService error", throwable);
        if (throwable instanceof ServiceException serviceException) {
            Error domainError = serviceException.error();
            return new Result.Error(domainError, command, throwable);
        }
        return new Result.Error(Errors.UpdateTransactionsErrors.PERSISTENCE_ERROR, command, throwable);
    }
}


