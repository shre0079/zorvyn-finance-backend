package com.shreyash.zorvyn.finance_dashboard_backend.controller;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.*;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.AccessDeniedException;
import com.shreyash.zorvyn.finance_dashboard_backend.mapper.TransactionMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard aggregation REST endpoints.
 * All endpoints require authentication.
 * VIEWER/ANALYST see their own data only.
 * ADMIN sees aggregate data across all users by default;
 * pass ?userId= to scope to a single user.
 * Soft-deleted transactions are excluded from all aggregations.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard", description = "Financial summary, trends, and analytics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Financial summary for a date range",
            description = "Returns totalIncome, totalExpenses, netBalance, " +
                    "transactionCount, averageTransaction. Defaults to current month."
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "ADMIN only — scope to a specific user")
            @RequestParam(required = false) UUID userId) {

        User caller = securityContextHelper.getCurrentUser();
        enforceAdminForUserFilter(userId, caller.getRole());

        DashboardSummaryResponse result = dashboardService.getSummary(
                startDate, endDate, caller.getId(), caller.getRole(), userId);

        return ResponseEntity.ok(ApiResponse.success("Summary retrieved successfully", result));
    }

    @GetMapping("/category-breakdown")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Spending/income breakdown by category",
            description = "Returns each category's total amount, percentage of grand total, " +
                    "and transaction count. Sorted descending by totalAmount."
    )
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(required = false) UUID userId) {

        User caller = securityContextHelper.getCurrentUser();
        enforceAdminForUserFilter(userId, caller.getRole());

        List<CategoryBreakdownResponse> result = dashboardService.getCategoryBreakdown(
                startDate, endDate, caller.getId(), caller.getRole(), userId);

        return ResponseEntity.ok(
                ApiResponse.success("Category breakdown retrieved successfully", result));
    }

    @GetMapping("/monthly-trend")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Monthly income/expense trend for a year",
            description = "Returns all 12 months. Months with no transactions have zero values."
    )
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getMonthlyTrend(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}")
            @Min(2000) @Max(2100) int year,

            @RequestParam(required = false) UUID userId) {

        User caller = securityContextHelper.getCurrentUser();
        enforceAdminForUserFilter(userId, caller.getRole());

        List<MonthlyTrendResponse> result = dashboardService.getMonthlyTrend(
                year, caller.getId(), caller.getRole(), userId);

        return ResponseEntity.ok(
                ApiResponse.success("Monthly trend retrieved successfully", result));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Most recent transactions",
            description = "Returns the N most recently dated non-deleted transactions."
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecentActivity(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
            @RequestParam(required = false) UUID userId) {

        User caller = securityContextHelper.getCurrentUser();
        enforceAdminForUserFilter(userId, caller.getRole());

        List<TransactionResponse> result = dashboardService.getRecentActivity(
                limit, caller.getId(), caller.getRole(), userId, transactionMapper);

        return ResponseEntity.ok(
                ApiResponse.success("Recent activity retrieved successfully", result));
    }

    @GetMapping("/top-categories")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Top N categories by total amount",
            description = "Default: top 5 EXPENSE categories. Pass ?type=INCOME for income side."
    )
    public ResponseEntity<ApiResponse<List<TopCategoryResponse>>> getTopCategories(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
            @RequestParam(defaultValue = "EXPENSE") TransactionType type,
            @RequestParam(required = false) UUID userId) {

        User caller = securityContextHelper.getCurrentUser();
        enforceAdminForUserFilter(userId, caller.getRole());

        List<TopCategoryResponse> result = dashboardService.getTopCategories(
                limit, type, caller.getId(), caller.getRole(), userId);

        return ResponseEntity.ok(
                ApiResponse.success("Top categories retrieved successfully", result));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Non-ADMIN callers must not pass a ?userId= filter.
     * Throw 403 immediately to prevent probing.
     */
    private void enforceAdminForUserFilter(UUID userId, UserRole callerRole) {
        if (userId != null && callerRole != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Only ADMIN users may filter dashboard data by userId.");
        }
    }
}
