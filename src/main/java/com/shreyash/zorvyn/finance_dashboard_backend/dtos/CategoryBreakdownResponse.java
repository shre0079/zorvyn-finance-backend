package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * One item in the GET /api/dashboard/category-breakdown list.
 * Sorted descending by totalAmount in the service layer.
 */

@Data
@Builder
public class CategoryBreakdownResponse {

    private TransactionCategory category;
    private BigDecimal totalAmount;

    /** percentage = (totalAmount / grandTotal) * 100, rounded to 2 dp. */
    private BigDecimal percentage;

    private long count;
}
