package com.shreyash.zorvyn.finance_dashboard_backend.utilities;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Helpers for computing default date ranges used in dashboard endpoints.
 *
 * When no startDate/endDate are provided, dashboard endpoints default
 * to the current calendar month (first day → last day of current month).
 */
public final class DateRangeUtil {

    private DateRangeUtil() { /* static utility */ }

    /** Returns the first day of the current calendar month. */
    public static LocalDate startOfCurrentMonth() {
        return YearMonth.now().atDay(1);
    }

    /** Returns the last day of the current calendar month. */
    public static LocalDate endOfCurrentMonth() {
        return YearMonth.now().atEndOfMonth();
    }

    /**
     * Returns startDate if non-null, otherwise defaults to the first day
     * of the current month.
     */
    public static LocalDate resolveStart(LocalDate startDate) {
        return startDate != null ? startDate : startOfCurrentMonth();
    }

    /**
     * Returns endDate if non-null, otherwise defaults to the last day
     * of the current month.
     */
    public static LocalDate resolveEnd(LocalDate endDate) {
        return endDate != null ? endDate : endOfCurrentMonth();
    }

    /**
     * Validates that start is not after end.
     *
     * @throws IllegalArgumentException if the range is inverted
     */
    public static void validateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "startDate (" + start + ") must not be after endDate (" + end + ")");
        }
    }
}
