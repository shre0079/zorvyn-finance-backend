package com.shreyash.zorvyn.finance_dashboard_backend.mapper;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.CreateTransactionRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.TransactionResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.UpdateTransactionRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransactionMapper {

    /**
     * Entity → response DTO.
     * Flattens user.id → createdBy.userId and user.email → createdBy.email.
     */
    @Mapping(target = "createdBy.userId", source = "user.id")
    @Mapping(target = "createdBy.email",  source = "user.email")
    TransactionResponse toResponse(Transaction transaction);

    /**
     * CreateTransactionRequest → entity.
     * user, id, isDeleted, createdAt, updatedAt are set by the service.
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);

    /**
     * Partial update: applies only non-null fields from the update request
     * onto the existing Transaction entity.
     */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void partialUpdate(
            @MappingTarget Transaction target,
            UpdateTransactionRequest source);
}
