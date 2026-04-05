package com.shreyash.zorvyn.finance_dashboard_backend.repositories;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.request.TransactionFilterRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.Transaction;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecification {

    private TransactionSpecification() { /* static utility class */ }

    /**
     * Builds a Specification from the filter request.
     *
     * @param filter       query parameters from the request
     * @param callerUserId UUID of the authenticated caller
     * @param callerRole   role of the authenticated caller
     */
    public static Specification<Transaction> build(
            TransactionFilterRequest filter,
            UUID callerUserId,
            UserRole callerRole) {

        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ── Soft-delete: always exclude deleted records for non-ADMIN ──
            if (callerRole != UserRole.ADMIN) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            // ADMIN can still choose to exclude deleted by default
            if (callerRole == UserRole.ADMIN) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }

            // ── Ownership scoping ──────────────────────────────────────────
            if (callerRole == UserRole.ADMIN && filter.getUserId() != null) {
                // Admin filtered to a specific user
                Join<Transaction, User> userJoin = root.join("user", JoinType.INNER);
                predicates.add(cb.equal(userJoin.get("id"), filter.getUserId()));
            } else if (callerRole != UserRole.ADMIN) {
                // VIEWER / ANALYST see only their own
                Join<Transaction, User> userJoin = root.join("user", JoinType.INNER);
                predicates.add(cb.equal(userJoin.get("id"), callerUserId));
            }
            // ADMIN with no userId filter: sees everything (no predicate added)

            // ── Type filter ────────────────────────────────────────────────
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            // ── Category filter ────────────────────────────────────────────
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            // ── Date range ─────────────────────────────────────────────────
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("transactionDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("transactionDate"), filter.getEndDate()));
            }

            // ── Amount range ───────────────────────────────────────────────
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("amount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("amount"), filter.getMaxAmount()));
            }

            // ── Description fuzzy search (ILIKE) ──────────────────────────
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + filter.getSearch().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
