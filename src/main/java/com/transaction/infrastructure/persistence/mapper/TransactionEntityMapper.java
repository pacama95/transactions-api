package com.transaction.infrastructure.persistence.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI)
public interface TransactionEntityMapper {
    @Mapping(target = "costPerShare", source = "price")
    @Mapping(target = "commission", source = "fees")
    @Mapping(target = "exchange", source = "exchange")
    @Mapping(target = "country", source = "country")
    TransactionEntity toEntity(Transaction transaction);

    @Mapping(target = "price", source = "costPerShare")
    @Mapping(target = "fees", source = "commission")
    @Mapping(target = "exchange", source = "exchange")
    @Mapping(target = "country", source = "country")
    Transaction toDomain(TransactionEntity entity);

    default Transaction createTransaction(TransactionEntity transactionEntity) {
        return Transaction.create(
                transactionEntity.getId(),
                transactionEntity.getTicker(),
                transactionEntity.getTransactionType(),
                transactionEntity.getQuantity(),
                transactionEntity.getCostPerShare(),
                transactionEntity.getCommission(),
                transactionEntity.getCurrency(),
                transactionEntity.getTransactionDate(),
                transactionEntity.getNotes(),
                true,
                transactionEntity.getIsFractional(),
                transactionEntity.getFractionalMultiplier(),
                transactionEntity.getCommissionCurrency(),
                transactionEntity.getExchange(),
                transactionEntity.getCountry()
        );
    }
} 