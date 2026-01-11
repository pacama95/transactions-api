package com.transaction.infrastructure.outgoing.messaging.mapper;

import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.domain.event.TransactionDeletedEvent;
import com.transaction.domain.event.TransactionUpdatedEvent;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionDeletedData;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionUpdatedData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI)
public interface TransactionMessageMapper {

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "messageCreatedAt", expression = "java(generateMessageCreatedAt())")
    @Mapping(target = "eventType", constant = "TransactionCreated")
    @Mapping(target = "payload", source = "transactionCreatedEvent", qualifiedByName = "mapToTransactionCreatedPayload")
    Message<TransactionCreatedData> toTransactionCreated(TransactionCreatedEvent transactionCreatedEvent);

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "messageCreatedAt", expression = "java(generateMessageCreatedAt())")
    @Mapping(target = "eventType", constant = "TransactionUpdated")
    @Mapping(target = "payload", source = "transactionUpdatedEvent", qualifiedByName = "mapToTransactionUpdatedPayload")
    Message<TransactionUpdatedData> toTransactionUpdated(TransactionUpdatedEvent transactionUpdatedEvent);

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "messageCreatedAt", expression = "java(generateMessageCreatedAt())")
    @Mapping(target = "eventType", constant = "TransactionDeleted")
    @Mapping(target = "payload", source = "transactionDeletedEvent", qualifiedByName = "mapToTransactionDeletedPayload")
    Message<TransactionDeletedData> toTransactionDeleted(TransactionDeletedEvent transactionDeletedEvent);

    @org.mapstruct.Named("mapToTransactionCreatedPayload")
    @Mapping(target = "id", source = "data.id")
    @Mapping(target = "ticker", source = "data.ticker")
    @Mapping(target = "transactionType", source = "data.transactionType")
    @Mapping(target = "quantity", source = "data.quantity")
    @Mapping(target = "price", source = "data.price")
    @Mapping(target = "fees", source = "data.fees")
    @Mapping(target = "currency", source = "data.currency")
    @Mapping(target = "transactionDate", source = "data.transactionDate")
    @Mapping(target = "notes", source = "data.notes")
    @Mapping(target = "isFractional", source = "data.isFractional")
    @Mapping(target = "fractionalMultiplier", source = "data.fractionalMultiplier")
    @Mapping(target = "commissionCurrency", source = "data.commissionCurrency")
    @Mapping(target = "exchange", source = "data.exchange")
    @Mapping(target = "country", source = "data.country")
    @Mapping(target = "companyName", source = "data.companyName")
    TransactionCreatedData mapToTransactionCreatedPayload(TransactionCreatedEvent transactionCreatedEvent);

    @org.mapstruct.Named("mapToTransactionUpdatedPayload")
    @Mapping(target = "previousTransaction", source = "data.previousTransaction")
    @Mapping(target = "newTransaction", source = "data.newTransaction")
    TransactionUpdatedData mapToTransactionUpdatedPayload(TransactionUpdatedEvent transactionUpdatedEvent);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticker", source = "ticker")
    @Mapping(target = "transactionType", source = "transactionType")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "fees", source = "fees")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "transactionDate", source = "transactionDate")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "isFractional", source = "isFractional")
    @Mapping(target = "fractionalMultiplier", source = "fractionalMultiplier")
    @Mapping(target = "commissionCurrency", source = "commissionCurrency")
    @Mapping(target = "exchange", source = "exchange")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "companyName", source = "companyName")
    TransactionUpdatedData.TransactionSnapshot toTransactionSnapshot(com.transaction.domain.model.Transaction transaction);

    @org.mapstruct.Named("mapToTransactionDeletedPayload")
    @Mapping(target = "id", source = "data.id")
    @Mapping(target = "ticker", source = "data.ticker")
    @Mapping(target = "transactionType", source = "data.transactionType")
    @Mapping(target = "quantity", source = "data.quantity")
    @Mapping(target = "price", source = "data.price")
    @Mapping(target = "fees", source = "data.fees")
    @Mapping(target = "currency", source = "data.currency")
    @Mapping(target = "transactionDate", source = "data.transactionDate")
    @Mapping(target = "notes", source = "data.notes")
    @Mapping(target = "isFractional", source = "data.isFractional")
    @Mapping(target = "fractionalMultiplier", source = "data.fractionalMultiplier")
    @Mapping(target = "commissionCurrency", source = "data.commissionCurrency")
    @Mapping(target = "exchange", source = "data.exchange")
    @Mapping(target = "country", source = "data.country")
    @Mapping(target = "companyName", source = "data.companyName")
    TransactionDeletedData mapToTransactionDeletedPayload(TransactionDeletedEvent transactionDeletedEvent);

    default Instant generateMessageCreatedAt() {
        return Instant.now();
    }
}
