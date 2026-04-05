package com.shreyash.zorvyn.finance_dashboard_backend.utilities;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.TransactionFilterRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Helpers for building Spring Data {@link Pageable} objects from filter requests.
 *
 * Enforces the allowed sortBy field whitelist to prevent clients from
 * sorting on arbitrary (potentially sensitive or non-indexed) columns.
 */
public final class PaginationUtil {

    private PaginationUtil() { /* static utility */ }

    /** Fields that callers are permitted to sort by. */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("amount", "transactionDate", "category", "createdAt");

    /** Default sort field when the caller provides an invalid or null value. */
    private static final String DEFAULT_SORT_FIELD = "transactionDate";

    /**
     * Builds a {@link Pageable} from a {@link TransactionFilterRequest}.
     *
     * @param filter validated filter request (page, size, sortBy, sortDir)
     * @return Spring Data Pageable ready for use in repository calls
     */
    public static Pageable buildPageable(TransactionFilterRequest filter) {
        String sortField = resolveSortField(filter.getSortBy());
        Sort.Direction direction = resolveSortDirection(filter.getSortDir());
        return PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(direction, sortField));
    }

    /**
     * Builds a {@link Pageable} for simple limit queries (recent activity, top categories).
     *
     * @param limit   maximum number of results
     * @param sortBy  field name
     * @param dir     sort direction
     */
    public static Pageable buildLimitPageable(int limit, String sortBy, Sort.Direction dir) {
        String field = resolveSortField(sortBy);
        return PageRequest.of(0, limit, Sort.by(dir, field));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static String resolveSortField(String sortBy) {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return DEFAULT_SORT_FIELD;
        }
        return sortBy;
    }

    private static Sort.Direction resolveSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;  // default
    }
}