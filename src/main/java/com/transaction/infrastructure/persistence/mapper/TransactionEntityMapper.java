package com.transaction.infrastructure.persistence.mapper;

import com.transaction.domain.event.DomainEvent;
import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

@Mapper(componentModel = JAKARTA_CDI)
public interface TransactionEntityMapper {
    @Mapping(target = "costPerShare", source = "price")
    @Mapping(target = "commission", source = "fees")
    TransactionEntity toEntity(Transaction transaction);

    @Mapping(target = "price", source = "costPerShare")
    @Mapping(target = "fees", source = "commission")
    @Mapping(target = "domainEvents", expression = "java(new ArrayList<DomainEvent<?>>())")
    Transaction toDomain(TransactionEntity entity);

    @Mapping(target = "price", source = "entity.costPerShare")
    @Mapping(target = "fees", source = "entity.commission")
    Transaction toDomain(TransactionEntity entity, List<DomainEvent<?>> domainEvents);
} 