package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public-facing representation of a Transaction entity.
 * Includes a nested CreatedBy object so callers know which user owns the record
 * without needing a separate /users lookup.
 */

@Data
@Builder
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionCategory category;
    private LocalDate transactionDate;
    private String description;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Minimal owner info embedded in the response. */
    private CreatedBy createdBy;

    @Data
    @Builder
    public static class CreatedBy {
        private UUID userId;
        private String email;
    }
}
