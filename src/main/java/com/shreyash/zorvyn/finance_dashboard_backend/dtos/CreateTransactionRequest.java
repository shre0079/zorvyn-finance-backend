package com.shreyash.zorvyn.finance_dashboard_backend.dtos;


import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for POST /api/transactions.
 * Validation rules (per specification):
 *  - amount  : positive, non-null, max 13 digits before the decimal point.
 *  - type    : must be a valid TransactionType enum value.
 *  - category: must be a valid TransactionCategory enum value.
 *  - date    : valid, not in the future (future-date check handled in service).
 *  - description: optional, max 500 characters.
 */


@Data
public class CreateTransactionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2,
            message = "Amount must not exceed 13 digits before the decimal and 2 after")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Category is required")
    private TransactionCategory category;

    @NotNull(message = "Transaction date is required")
    // Note: @PastOrPresent ensures date is not in the future
    @PastOrPresent(message = "Transaction date must not be in the future")
    private LocalDate transactionDate;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
