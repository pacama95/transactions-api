package com.transaction.application.usecase.transaction;

import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreationErrorEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.exception.Error;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionError;
import com.transaction.domain.port.input.UpdateTransactionUseCase;
import com.transaction.domain.port.output.DomainEventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import com.transaction.util.StringUtils;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;

@ApplicationScoped
public class UpdateTransactionService implements UpdateTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    @Named("redisPublisher")
    DomainEventPublisher eventPublisher;

    @Override
    @WithTransaction
    public Uni<Result> execute(UpdateTransactionCommand command) {
        return transactionRepository.findById(command.transactionId())
                .flatMap(found -> {
                    if (found == null) {
                        return Uni.createFrom().item(new Result.NotFound());
                    }
                    return deleteAndCreateNew(found, command);
                })
                .onFailure().recoverWithItem(throwable -> transformToError(throwable, command));
    }

    private Uni<Result> deleteAndCreateNew(Transaction current, UpdateTransactionCommand command) {
        Transaction oldTransaction = createTransactionSnapshot(current);

        Transaction newTransaction = new Transaction(
                StringUtils.hasMeaningfulContent(command.ticker()) ? command.ticker() : current.getTicker(),
                command.transactionType() != null ? command.transactionType() : current.getTransactionType(),
                command.quantity() != null ? command.quantity() : current.getQuantity(),
                command.price() != null ? command.price() : current.getPrice(),
                command.fees() != null ? command.fees() : current.getFees(),
                command.currency() != null ? command.currency() : current.getCurrency(),
                command.transactionDate() != null ? command.transactionDate() : current.getTransactionDate(),
                StringUtils.hasMeaningfulContent(command.notes()) ? command.notes() : current.getNotes(),
                current.getIsActive(),
                command.isFractional() != null ? command.isFractional() : current.getIsFractional(),
                command.fractionalMultiplier() != null ? command.fractionalMultiplier() : current.getFractionalMultiplier(),
                command.commissionCurrency() != null ? command.commissionCurrency() : current.getCommissionCurrency(),
                StringUtils.hasMeaningfulContent(command.exchange()) ? command.exchange() : current.getExchange(),
                StringUtils.hasMeaningfulContent(command.country()) ? command.country() : current.getCountry()
        );

        return transactionRepository.deleteById(current.getId())
                .call(ignored -> eventPublisher.publish(new TransactionDeletedEvent(oldTransaction)))
                .flatMap(deleted -> transactionRepository.save(newTransaction))
                .map(Result.Success::new)
                .flatMap(result -> publishDomainEvents(result, command));
    }

    private Uni<UpdateTransactionUseCase.Result> publishDomainEvents(UpdateTransactionUseCase.Result result, UpdateTransactionCommand command) {
        if (result instanceof UpdateTransactionUseCase.Result.Success(Transaction transaction)) {
            var events = transaction.popEvents();

            if (events.isEmpty()) {
                return Uni.createFrom().item(() -> result);
            }

            return Multi.createFrom().iterable(events)
                    .onItem().transformToUniAndMerge(event -> eventPublisher.publish(event))
                    .collect().asList()
                    .replaceWith(() -> result)
                    .onFailure().recoverWithItem(throwable -> new UpdateTransactionUseCase.Result.PublishError(transaction, throwable));
        }

        if (result instanceof UpdateTransactionUseCase.Result.Error error) {
            TransactionError transactionError =
                    new TransactionError(command.ticker(), command.transactionType(), command.quantity(), command.price(), command.transactionDate());
            DomainEvent<TransactionError> errorEvent = new TransactionCreationErrorEvent(transactionError);

            return eventPublisher.publish(errorEvent).replaceWith(() -> error);
        }

        return Uni.createFrom().item(() -> result);
    }

    private Transaction createTransactionSnapshot(Transaction transaction) {
        return new Transaction(
                transaction.getId(),
                transaction.getTicker(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getFees(),
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getNotes(),
                transaction.getIsActive(),
                transaction.getIsFractional(),
                transaction.getFractionalMultiplier(),
                transaction.getCommissionCurrency(),
                transaction.getExchange(),
                transaction.getCountry(),
                new ArrayList<>()
        );
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


