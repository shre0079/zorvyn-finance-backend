package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * One item in the GET /api/dashboard/monthly-trend list.
 * All 12 months are always returned; months with no data have zero amounts.
 */

@Data
@Builder
public class MonthlyTrendResponse {

    /** Calendar month number 1–12. */
    private int month;

    /** Human-readable month name e.g. "January". */
    private String monthName;

    private BigDecimal income;
    private BigDecimal expenses;

    /** net = income - expenses (can be negative). */
    private BigDecimal net;
}
