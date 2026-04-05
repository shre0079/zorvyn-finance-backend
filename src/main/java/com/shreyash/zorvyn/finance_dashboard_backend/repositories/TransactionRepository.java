package com.shreyash.zorvyn.finance_dashboard_backend.repositories;


import com.shreyash.zorvyn.finance_dashboard_backend.entities.Transaction;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    // ── Basic lookups ──────────────────────────────────────────────────────

    /** Single transaction visible to ADMIN (any, including soft-deleted). */
    Optional<Transaction> findById(UUID id);

    /** Single active transaction — used by VIEWER/ANALYST for their own. */
    Optional<Transaction> findByIdAndIsDeletedFalse(UUID id);

    /** Single active transaction belonging to a specific user. */
    Optional<Transaction> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    // ── Paginated list queries ─────────────────────────────────────────────

    /** All non-deleted transactions (ADMIN view, no user filter). */
    Page<Transaction> findByIsDeletedFalse(Pageable pageable);

    /** Non-deleted transactions for a specific user. */
    Page<Transaction> findByUserIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    // ── Dashboard aggregation queries ──────────────────────────────────────

    /**
     * Sum of amounts grouped by type (INCOME/EXPENSE) for a user within a date range.
     * Excludes soft-deleted records.
     *
     * Returns List<Object[]> where [0]=TransactionType, [1]=BigDecimal sum.
     */
    @Query("""
            SELECT t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.type
           """)
    List<Object[]> sumByTypeForUser(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Same aggregation but across ALL users (ADMIN aggregate dashboard).
     */
    @Query("""
            SELECT t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.type
           """)
    List<Object[]> sumByTypeForAll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count of non-deleted transactions for a user within a date range.
     */
    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
           """)
    long countForUser(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count across all users (ADMIN).
     */
    @Query("""
            SELECT COUNT(t)
            FROM Transaction t
            WHERE t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
           """)
    long countForAll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ── Category breakdown ─────────────────────────────────────────────────

    @Query("""
            SELECT t.category, SUM(t.amount), COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
           """)
    List<Object[]> categoryBreakdownForUser(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT t.category, SUM(t.amount), COUNT(t)
            FROM Transaction t
            WHERE t.isDeleted = FALSE
              AND t.transactionDate BETWEEN :startDate AND :endDate
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
           """)
    List<Object[]> categoryBreakdownForAll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ── Monthly trend ──────────────────────────────────────────────────────

    /**
     * Returns rows of [month(int), type, sum] for a specific user and year.
     */
    @Query("""
            SELECT MONTH(t.transactionDate), t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
              AND YEAR(t.transactionDate) = :year
            GROUP BY MONTH(t.transactionDate), t.type
            ORDER BY MONTH(t.transactionDate)
           """)
    List<Object[]> monthlyTrendForUser(
            @Param("userId") UUID userId,
            @Param("year") int year);

    @Query("""
            SELECT MONTH(t.transactionDate), t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.isDeleted = FALSE
              AND YEAR(t.transactionDate) = :year
            GROUP BY MONTH(t.transactionDate), t.type
            ORDER BY MONTH(t.transactionDate)
           """)
    List<Object[]> monthlyTrendForAll(@Param("year") int year);

    // ── Top categories ─────────────────────────────────────────────────────

    @Query("""
            SELECT t.category, SUM(t.amount)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
              AND t.type = :type
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
           """)
    List<Object[]> topCategoriesForUser(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            Pageable pageable);

    @Query("""
            SELECT t.category, SUM(t.amount)
            FROM Transaction t
            WHERE t.isDeleted = FALSE
              AND t.type = :type
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
           """)
    List<Object[]> topCategoriesForAll(
            @Param("type") TransactionType type,
            Pageable pageable);

    // ── Recent activity ────────────────────────────────────────────────────

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND t.isDeleted = FALSE
            ORDER BY t.transactionDate DESC, t.createdAt DESC
           """)
    List<Transaction> recentActivityForUser(
            @Param("userId") UUID userId,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.isDeleted = FALSE
            ORDER BY t.transactionDate DESC, t.createdAt DESC
           """)
    List<Transaction> recentActivityForAll(Pageable pageable);

    // ── Soft-delete ────────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Transaction t SET t.isDeleted = TRUE WHERE t.id = :id")
    int softDeleteById(@Param("id") UUID id);
}
