package com.shreyash.zorvyn.finance_dashboard_backend.services;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.*;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.Transaction;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.AccessDeniedException;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.ResourceNotFoundException;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.ValidationException;
import com.shreyash.zorvyn.finance_dashboard_backend.mapper.TransactionMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.TransactionRepository;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.TransactionSpecification;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.UserRepository;
import com.shreyash.zorvyn.finance_dashboard_backend.utilities.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core transaction CRUD and access-control logic.
 *
 * Access rules (per spec):
 *  CREATE  – ANALYST and ADMIN only.
 *  READ    – all roles; VIEWER/ANALYST see only their own (non-deleted).
 *            ADMIN sees all (non-deleted); soft-deleted visible only via explicit ADMIN lookup.
 *  UPDATE  – ANALYST (own only), ADMIN (any).
 *  DELETE  – soft-delete only; ANALYST (own only), ADMIN (any).
 *
 * Access violations always return 403 — never 404 — regardless of whether
 * the resource actually exists, to avoid leaking information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    // ── Create ────────────────────────────────────────────────────────────

    /**
     * Creates a new transaction owned by the caller.
     * Date validation (not in future) is handled by @PastOrPresent on the DTO,
     * but we also double-check here for programmatic callers.
     */
    @Transactional
    public TransactionResponse createTransaction(
            CreateTransactionRequest request, UUID callerUserId) {

        // Extra guard — @PastOrPresent covers the REST path but not programmatic callers
        if (request.getTransactionDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date must not be in the future.");
        }

        User owner = findUserOrThrow(callerUserId);

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUser(owner);

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: {} by user {}", saved.getId(), callerUserId);
        return transactionMapper.toResponse(saved);
    }

    // ── Read (paginated list) ─────────────────────────────────────────────

    /**
     * Returns a filtered, paginated list of transactions.
     *
     * Scoping:
     *  VIEWER / ANALYST → only their own non-deleted transactions.
     *  ADMIN            → all non-deleted transactions; optional ?userId= filter.
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactions(
            TransactionFilterRequest filter,
            UUID callerUserId,
            UserRole callerRole) {

        Pageable pageable = PaginationUtil.buildPageable(filter);

        Page<Transaction> page = transactionRepository.findAll(
                TransactionSpecification.build(filter, callerUserId, callerRole),
                pageable);

        List<TransactionResponse> content = page.getContent()
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.from(page, content);
    }

    // ── Read (single) ─────────────────────────────────────────────────────

    /**
     * Retrieves a single transaction by ID.
     *
     * VIEWER / ANALYST: may only access their own non-deleted transactions.
     *   - If it doesn't exist → 403 (not 404, to avoid leaking existence).
     *   - If it exists but belongs to someone else → 403.
     * ADMIN: may access any non-deleted transaction.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(
            UUID id, UUID callerUserId, UserRole callerRole) {

        if (callerRole == UserRole.ADMIN) {
            Transaction tx = transactionRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
            return transactionMapper.toResponse(tx);
        }

        // VIEWER / ANALYST — always 403 if they cannot see it
        Transaction tx = transactionRepository
                .findByIdAndUserIdAndIsDeletedFalse(id, callerUserId)
                .orElseThrow(AccessDeniedException::new);

        return transactionMapper.toResponse(tx);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Partial update of an existing transaction.
     *
     * ANALYST: may only update their own non-deleted transactions.
     * ADMIN:   may update any non-deleted transaction.
     */
    @Transactional
    public TransactionResponse updateTransaction(
            UUID id,
            UpdateTransactionRequest request,
            UUID callerUserId,
            UserRole callerRole) {

        Transaction transaction = resolveEditableTransaction(id, callerUserId, callerRole);

        // Additional future-date guard for updates
        if (request.getTransactionDate() != null
                && request.getTransactionDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Transaction date must not be in the future.");
        }

        transactionMapper.partialUpdate(transaction, request);
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction {} updated by user {}", id, callerUserId);
        return transactionMapper.toResponse(saved);
    }

    // ── Soft Delete ───────────────────────────────────────────────────────

    /**
     * Soft-deletes a transaction (sets is_deleted = true).
     * Physical deletion never occurs.
     *
     * ANALYST: may only soft-delete their own transactions.
     * ADMIN:   may soft-delete any transaction.
     */
    @Transactional
    public void deleteTransaction(UUID id, UUID callerUserId, UserRole callerRole) {
        resolveEditableTransaction(id, callerUserId, callerRole); // access check

        int updated = transactionRepository.softDeleteById(id);
        if (updated == 0) {
            throw new ResourceNotFoundException("Transaction", "id", id);
        }
        log.info("Transaction {} soft-deleted by user {}", id, callerUserId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Resolves a transaction for edit/delete, enforcing ownership rules.
     *
     * ADMIN → any non-deleted transaction (404 if not found).
     * ANALYST → own non-deleted transactions only (403 if not found or not owner).
     */
    private Transaction resolveEditableTransaction(
            UUID id, UUID callerUserId, UserRole callerRole) {

        if (callerRole == UserRole.ADMIN) {
            return transactionRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        }

        // ANALYST — returns 403 regardless of whether the tx exists or belongs to someone else
        return transactionRepository
                .findByIdAndUserIdAndIsDeletedFalse(id, callerUserId)
                .orElseThrow(AccessDeniedException::new);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
