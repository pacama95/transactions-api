package com.transaction.infrastructure.incoming.mcp.mapper;

import com.transaction.domain.model.Transaction;
import com.transaction.infrastructure.incoming.mcp.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface TransactionMapper {
    
    @Mapping(target = "transactionType", source = "transactionType")
    @Mapping(target = "transactionDate", source = "transactionDate")
    @Mapping(target = "companyName", source = "companyName")
    TransactionDto toDto(Transaction transaction);
}
