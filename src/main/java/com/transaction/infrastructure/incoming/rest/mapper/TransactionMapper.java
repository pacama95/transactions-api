package com.transaction.infrastructure.incoming.rest.mapper;

import com.transaction.application.command.CreateTransactionCommand;
import com.transaction.application.command.UpdateTransactionCommand;
import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.incoming.rest.dto.CreateTransactionRequest;
import com.transaction.infrastructure.incoming.rest.dto.TransactionResponse;
import com.transaction.infrastructure.incoming.rest.dto.UpdateTransactionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "cdi")
public interface TransactionMapper {
    int MONETARY_SCALE = 4;
    int QUANTITY_SCALE = 6;
    RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Mapping(target = "quantity", expression = "java(normalizeQuantity(transaction.getQuantity()))")
    @Mapping(target = "price", expression = "java(normalizeMonetary(transaction.getPrice()))")
    @Mapping(target = "fees", expression = "java(normalizeMonetary(transaction.getFees()))")
    @Mapping(target = "totalValue", expression = "java(normalizeMonetary(transaction.getTotalValue()))")
    @Mapping(target = "totalCost", expression = "java(normalizeMonetary(transaction.getTotalCost()))")
    @Mapping(target = "fractionalMultiplier", expression = "java(normalizeMonetary(transaction.getFractionalMultiplier()))")
    @Mapping(target = "exchange", source = "exchange")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "companyName", source = "companyName")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponses(List<Transaction> transactions);

    @Mapping(target = "quantity", expression = "java(normalizeQuantity(createTransactionRequest.quantity()))")
    @Mapping(target = "price", expression = "java(normalizeMonetary(createTransactionRequest.price()))")
    @Mapping(target = "fees", expression = "java(normalizeMonetary(createTransactionRequest.fees()))")
    @Mapping(target = "fractionalMultiplier", expression = "java(normalizeMonetary(createTransactionRequest.fractionalMultiplier()))")
    @Mapping(target = "exchange", source = "exchange")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "companyName", source = "companyName")
    CreateTransactionCommand toCreateTransactionCommand(CreateTransactionRequest createTransactionRequest);

    @Mapping(target = "quantity", expression = "java(normalizeQuantity(updateTransactionRequest.quantity()))")
    @Mapping(target = "price", expression = "java(normalizeMonetary(updateTransactionRequest.price()))")
    @Mapping(target = "fees", expression = "java(normalizeMonetary(updateTransactionRequest.fees()))")
    @Mapping(target = "fractionalMultiplier", expression = "java(normalizeMonetary(updateTransactionRequest.fractionalMultiplier()))")
    @Mapping(target = "exchange", source = "updateTransactionRequest.exchange")
    @Mapping(target = "country", source = "updateTransactionRequest.country")
    @Mapping(target = "companyName", source = "updateTransactionRequest.companyName")
    UpdateTransactionCommand toUpdateTransactionCommand(UUID transactionId, UpdateTransactionRequest updateTransactionRequest);

    // Normalization helpers
    default BigDecimal normalizeMonetary(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(MONETARY_SCALE, ROUNDING);
    }

    default BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(QUANTITY_SCALE, ROUNDING);
    }
} 