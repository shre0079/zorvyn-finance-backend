package com.shreyash.zorvyn.finance_dashboard_backend.services;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.*;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.Transaction;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionCategory;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import com.shreyash.zorvyn.finance_dashboard_backend.mapper.TransactionMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.TransactionRepository;
import com.shreyash.zorvyn.finance_dashboard_backend.utilities.DateRangeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * All dashboard aggregation logic.
 *
 * Scoping rules (applied uniformly across all endpoints):
 *  VIEWER / ANALYST → their own data only.
 *  ADMIN            → aggregate across all users by default;
 *                     optional userId parameter scopes to one user.
 *
 * Soft-deleted transactions are ALWAYS excluded from all aggregations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TransactionRepository transactionRepository;

    // ── Summary ───────────────────────────────────────────────────────────

    /**
     * Returns totalIncome, totalExpenses, netBalance, transactionCount,
     * and averageTransaction for the given period.
     *
     * Defaults to current calendar month when no dates are provided.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(
            LocalDate startDate, LocalDate endDate,
            UUID callerUserId, UserRole callerRole, UUID filterUserId) {

        LocalDate start = DateRangeUtil.resolveStart(startDate);
        LocalDate end   = DateRangeUtil.resolveEnd(endDate);
        DateRangeUtil.validateRange(start, end);

        boolean adminViewAll = callerRole == UserRole.ADMIN && filterUserId == null;
        UUID targetUserId = resolveTargetUserId(callerUserId, callerRole, filterUserId);

        // Fetch sums grouped by type
        List<Object[]> sums = adminViewAll
                ? transactionRepository.sumByTypeForAll(start, end)
                : transactionRepository.sumByTypeForUser(targetUserId, start, end);

        BigDecimal totalIncome   = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Object[] row : sums) {
            TransactionType type   = (TransactionType) row[0];
            BigDecimal      amount = (BigDecimal) row[1];
            if (type == TransactionType.INCOME) {
                totalIncome = amount;
            } else {
                totalExpenses = amount;
            }
        }

        long count = adminViewAll
                ? transactionRepository.countForAll(start, end)
                : transactionRepository.countForUser(targetUserId, start, end);

        BigDecimal net     = totalIncome.subtract(totalExpenses);
        BigDecimal total   = totalIncome.add(totalExpenses);
        BigDecimal average = count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome.setScale(2, RoundingMode.HALF_UP))
                .totalExpenses(totalExpenses.setScale(2, RoundingMode.HALF_UP))
                .netBalance(net.setScale(2, RoundingMode.HALF_UP))
                .transactionCount(count)
                .averageTransaction(average)
                .period(DashboardSummaryResponse.Period.builder()
                        .startDate(start)
                        .endDate(end)
                        .build())
                .build();
    }

    // ── Category Breakdown ────────────────────────────────────────────────

    /**
     * Returns spending/income per category, sorted descending by totalAmount,
     * with each category's percentage of the grand total.
     */
    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(
            LocalDate startDate, LocalDate endDate,
            UUID callerUserId, UserRole callerRole, UUID filterUserId) {

        LocalDate start = DateRangeUtil.resolveStart(startDate);
        LocalDate end   = DateRangeUtil.resolveEnd(endDate);
        DateRangeUtil.validateRange(start, end);

        boolean adminViewAll = callerRole == UserRole.ADMIN && filterUserId == null;
        UUID    targetUserId = resolveTargetUserId(callerUserId, callerRole, filterUserId);

        List<Object[]> rows = adminViewAll
                ? transactionRepository.categoryBreakdownForAll(start, end)
                : transactionRepository.categoryBreakdownForUser(targetUserId, start, end);

        // Compute grand total first (for percentage)
        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    TransactionCategory category = (TransactionCategory) row[0];
                    BigDecimal          amount   = (BigDecimal) row[1];
                    long                count    = ((Number) row[2]).longValue();

                    BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : amount.multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 2, RoundingMode.HALF_UP);

                    return CategoryBreakdownResponse.builder()
                            .category(category)
                            .totalAmount(amount.setScale(2, RoundingMode.HALF_UP))
                            .percentage(percentage)
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
        // Already sorted by repository (ORDER BY SUM DESC)
    }

    // ── Monthly Trend ─────────────────────────────────────────────────────

    /**
     * Returns income, expenses, and net for all 12 months of the given year.
     * Months with no transactions return zero values (never omitted).
     */
    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrend(
            int year, UUID callerUserId, UserRole callerRole, UUID filterUserId) {

        boolean adminViewAll = callerRole == UserRole.ADMIN && filterUserId == null;
        UUID    targetUserId = resolveTargetUserId(callerUserId, callerRole, filterUserId);

        List<Object[]> rows = adminViewAll
                ? transactionRepository.monthlyTrendForAll(year)
                : transactionRepository.monthlyTrendForUser(targetUserId, year);

        // Build a lookup: month → {INCOME: amount, EXPENSE: amount}
        Map<Integer, Map<TransactionType, BigDecimal>> lookup = new HashMap<>();
        for (Object[] row : rows) {
            int             month  = ((Number) row[0]).intValue();
            TransactionType type   = (TransactionType) row[1];
            BigDecimal      amount = (BigDecimal) row[2];
            lookup.computeIfAbsent(month, k -> new HashMap<>()).put(type, amount);
        }

        // Build all 12 months, filling zeros for missing data
        List<MonthlyTrendResponse> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            Map<TransactionType, BigDecimal> monthData =
                    lookup.getOrDefault(m, Collections.emptyMap());

            BigDecimal income   = monthData.getOrDefault(TransactionType.INCOME,   BigDecimal.ZERO);
            BigDecimal expenses = monthData.getOrDefault(TransactionType.EXPENSE,  BigDecimal.ZERO);
            BigDecimal net      = income.subtract(expenses);

            result.add(MonthlyTrendResponse.builder()
                    .month(m)
                    .monthName(Month.of(m).name().charAt(0)
                            + Month.of(m).name().substring(1).toLowerCase())
                    .income(income.setScale(2, RoundingMode.HALF_UP))
                    .expenses(expenses.setScale(2, RoundingMode.HALF_UP))
                    .net(net.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return result;
    }

    // ── Recent Activity ───────────────────────────────────────────────────

    /**
     * Returns the N most recently dated non-deleted transactions.
     * Ties broken by createdAt descending.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentActivity(
            int limit, UUID callerUserId, UserRole callerRole, UUID filterUserId,
            TransactionMapper transactionMapper) {

        boolean adminViewAll = callerRole == UserRole.ADMIN && filterUserId == null;
        UUID    targetUserId = resolveTargetUserId(callerUserId, callerRole, filterUserId);

        PageRequest pageRequest = PageRequest.of(0, limit);

        List<Transaction> txList = adminViewAll
                ? transactionRepository.recentActivityForAll(pageRequest)
                : transactionRepository.recentActivityForUser(targetUserId, pageRequest);

        return txList.stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Top Categories ────────────────────────────────────────────────────

    /**
     * Returns the top N categories by total amount for the given transaction type.
     * Rank is 1-based. Percentage is relative to the total of that type.
     */
    @Transactional(readOnly = true)
    public List<TopCategoryResponse> getTopCategories(
            int limit, TransactionType type,
            UUID callerUserId, UserRole callerRole, UUID filterUserId) {

        boolean adminViewAll = callerRole == UserRole.ADMIN && filterUserId == null;
        UUID    targetUserId = resolveTargetUserId(callerUserId, callerRole, filterUserId);

        PageRequest pageRequest = PageRequest.of(0, limit);

        List<Object[]> rows = adminViewAll
                ? transactionRepository.topCategoriesForAll(type, pageRequest)
                : transactionRepository.topCategoriesForUser(targetUserId, type, pageRequest);

        // Grand total for the given type (sum of all returned rows)
        BigDecimal grandTotal = rows.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopCategoryResponse> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[]            row      = rows.get(i);
            TransactionCategory category = (TransactionCategory) row[0];
            BigDecimal          amount   = (BigDecimal) row[1];

            BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP);

            result.add(TopCategoryResponse.builder()
                    .rank(i + 1)
                    .category(category)
                    .totalAmount(amount.setScale(2, RoundingMode.HALF_UP))
                    .percentage(percentage)
                    .build());
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Resolves the effective target userId for scoped queries.
     *
     * - ADMIN with a filterUserId → use filterUserId.
     * - ADMIN with no filter      → returns null (signals "all users").
     * - Non-ADMIN                 → always callerUserId (own data only).
     */
    private UUID resolveTargetUserId(UUID callerUserId, UserRole callerRole, UUID filterUserId) {
        if (callerRole == UserRole.ADMIN) {
            return filterUserId; // may be null → all-user aggregation handled by callers
        }
        return callerUserId;
    }
}
