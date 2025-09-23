package com.transaction.application.usecase.transaction;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionCreationErrorEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.model.Transaction;
import com.transaction.domain.model.TransactionError;
import com.transaction.domain.port.output.EventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class CreateTransactionService implements com.transaction.domain.port.input.CreateTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    @Named("redisPublisher")
    EventPublisher<DomainEvent<?>> eventPublisher;

    @WithTransaction
    public Uni<Result> execute(CreateTransactionCommand command) {
        return Uni.createFrom().item(command::toTransaction)
                .flatMap(transaction -> transactionRepository.save(transaction))
                .onItem().invoke(saved -> Log.info("Transaction saved for ticker %s".formatted(saved.getTicker())))
                .map(CreateTransactionService::success)
                .onFailure().recoverWithItem(throwable -> transformToError(throwable, command))
                .flatMap(this::publishDomainEvents);
    }

    private Uni<Result> publishDomainEvents(Result result) {
        if (result instanceof Result.Success(Transaction transaction)) {
            var events = transaction.popEvents(); // Get and clear domain events

            if (events.isEmpty()) {
                return Uni.createFrom().item(() -> result);
            }

            return Multi.createFrom().iterable(events)
                    .onItem().transformToUniAndMerge(event -> eventPublisher.publish(event))
                    .collect().asList()
                    .replaceWith(() -> result)
                    .onFailure().recoverWithItem(throwable -> new Result.PublishError(transaction, throwable));
        }

        if (result instanceof Result.Error error) {
            CreateTransactionCommand command = error.command();
            TransactionError transactionError =
                    new TransactionError(command.ticker(), command.transactionType(), command.quantity(), command.price(), command.transactionDate());
            DomainEvent<TransactionError> errorEvent = new TransactionCreationErrorEvent(transactionError);

            return eventPublisher.publish(errorEvent)
                    .replaceWith(() -> error);
        }

        return Uni.createFrom().item(() -> result);
    }

    private static Result success(Transaction transaction) {
        return new Result.Success(transaction);
    }

    private static Result transformToError(Throwable throwable, CreateTransactionCommand command) {
        if (throwable instanceof ServiceException serviceException) {
            return new Result.Error(serviceException.error(), command, throwable);
        }

        return new Result.Error(Errors.CreateTransactionsErrors.GENERAL_ERROR, command, throwable);
    }
} 