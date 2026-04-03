package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Query parameters for GET /api/transactions.
 * Bound via @ModelAttribute in the controller.
 * All fields are optional; defaults are applied in the service.
 *
 * Allowed sortBy values: amount, transactionDate, category, createdAt.
 * Validated via @Pattern to prevent SQL-injection-style field manipulation.
 */

@Data
public class TransactionFilterRequest {

    /** ADMIN only: filter transactions by a specific user's UUID. */
    private UUID userId;

    // ── Pagination ──
    @Min(value = 0, message = "Page index must be >= 0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 100, message = "Page size must be <= 100")
    private int size = 10;

    @Pattern(
            regexp = "amount|transactionDate|category|createdAt",
            message = "sortBy must be one of: amount, transactionDate, category, createdAt"
    )
    private String sortBy = "transactionDate";

    @Pattern(
            regexp = "asc|desc",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "sortDir must be 'asc' or 'desc'"
    )
    private String sortDir = "desc";

    // Filters
    private TransactionType type;
    private TransactionCategory category;
    private LocalDate startDate;
    private LocalDate endDate;

    @DecimalMin(value = "0.00", message = "minAmount must be >= 0")
    private BigDecimal minAmount;

    @DecimalMin(value = "0.00", message = "maxAmount must be >= 0")
    private BigDecimal maxAmount;

    /** Fuzzy (ILIKE) match against the description field. */
    private String search;
}
