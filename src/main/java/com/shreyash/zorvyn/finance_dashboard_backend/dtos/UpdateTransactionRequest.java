package com.shreyash.zorvyn.finance_dashboard_backend.dtos;


import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for PUT /api/transactions/{id}.
 * All fields are optional — only non-null fields will be applied.
 */

@Data
public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2,
            message = "Amount must not exceed 13 digits before the decimal and 2 after")
    private BigDecimal amount;

    private TransactionType type;

    private TransactionCategory category;

    @PastOrPresent(message = "Transaction date must not be in the future")
    private LocalDate transactionDate;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

