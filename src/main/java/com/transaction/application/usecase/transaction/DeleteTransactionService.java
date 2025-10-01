package com.transaction.application.usecase.transaction;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import com.transaction.domain.port.input.DeleteTransactionUseCase;
import com.transaction.domain.port.output.DomainEventPublisher;
import com.transaction.domain.port.output.TransactionRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DeleteTransactionService implements DeleteTransactionUseCase {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    @Named("redisPublisher")
    DomainEventPublisher eventPublisher;

    @Override
    @WithTransaction
    public Uni<Result> execute(UUID id) {
        return transactionRepository.findById(id)
                .onFailure().recoverWithItem(throwable -> {
                    throw new ServiceException(Errors.DeleteTransactionsErrors.PERSISTENCE_ERROR, throwable);
                })
                .flatMap(found -> {
                    if (found == null) {
                        return Uni.createFrom().item(new Result.NotFound(id));
                    }
                    return transactionRepository.deleteById(id)
                            .flatMap(deleted -> {
                                if (deleted) {
                                    TransactionDeletedEvent event = new TransactionDeletedEvent(found);
                                    return publishEvents(List.of(event))
                                            .onItem().transform(ignored -> (DeleteTransactionUseCase.Result) new Result.Success(id))
                                            .onFailure().recoverWithItem(t -> new Result.PublishError(id, t));
                                } else {
                                    return Uni.createFrom().item(new Result.Error(Errors.DeleteTransactionsErrors.PERSISTENCE_ERROR, id, new RuntimeException("Delete returned false")));
                                }
                            });
                })
                .onFailure().recoverWithItem(t -> new Result.Error(Errors.DeleteTransactionsErrors.PERSISTENCE_ERROR, id, t));
    }

    private Uni<Void> publishEvents(List<? extends DomainEvent<?>> events) {
        if (events == null || events.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return Multi.createFrom().iterable(events)
                .onItem().transformToUniAndMerge(event -> eventPublisher.publish(event))
                .collect().asList()
                .replaceWithVoid();
    }
}


