package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response for GET /api/dashboard/summary.
 */

@Data
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private long transactionCount;
    private BigDecimal averageTransaction;
    private Period period;

    @Data
    @Builder
    public static class Period {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
