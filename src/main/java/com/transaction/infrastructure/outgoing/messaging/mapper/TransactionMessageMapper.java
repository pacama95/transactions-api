package com.transaction.infrastructure.outgoing.messaging.mapper;

import com.transaction.domain.event.TransactionCreatedEvent;
import com.transaction.infrastructure.outgoing.messaging.message.Message;
import com.transaction.infrastructure.outgoing.messaging.message.TransactionCreatedData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI)
public interface TransactionMessageMapper {

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "messageCreatedAt", expression = "java(generateMessageCreatedAt())")
    @Mapping(target = "payload", source = "transactionCreatedEvent", qualifiedByName = "mapToPayload")
    Message<TransactionCreatedData> toTransactionCreated(TransactionCreatedEvent transactionCreatedEvent);

    @org.mapstruct.Named("mapToPayload")
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
    TransactionCreatedData mapToPayload(TransactionCreatedEvent transactionCreatedEvent);

    default Instant generateMessageCreatedAt() {
        return Instant.now();
    }
}
