package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * One item in the GET /api/dashboard/top-categories list.
 */

@Data
@Builder
public class TopCategoryResponse {

    /** 1-based rank position. */
    private int rank;

    private TransactionCategory category;
    private BigDecimal totalAmount;

    /** percentage of the total for the given type (INCOME or EXPENSE). */
    private BigDecimal percentage;
}
