package com.shreyash.zorvyn.finance_dashboard_backend.controller;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.*;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Transaction CRUD REST endpoints.
 * Access control summary:
 *  POST   → ANALYST and ADMIN
 *  GET    → All authenticated (scoped in service layer)
 *  PUT    → ANALYST (own) and ADMIN (any)
 *  DELETE → ANALYST (own soft-delete) and ADMIN (any soft-delete)
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transactions", description = "Create, read, update, and soft-delete transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityContextHelper securityContextHelper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(
            summary = "Create a transaction",
            description = "ANALYST and ADMIN only. Transaction is owned by the caller."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        UUID callerUserId = securityContextHelper.getCurrentUserId();
        TransactionResponse result = transactionService.createTransaction(request, callerUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", result));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "List transactions (paginated + filtered)",
            description = """
            VIEWER and ANALYST see only their own non-deleted transactions.
            ADMIN sees all (pass ?userId= to scope to one user).
            Supports filtering by type, category, date range, amount range, and description search.
            """
    )
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @Valid @ModelAttribute TransactionFilterRequest filter) {

        User caller = securityContextHelper.getCurrentUser();
        PagedResponse<TransactionResponse> result = transactionService.getTransactions(
                filter, caller.getId(), caller.getRole());

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get transaction by ID",
            description = "VIEWER/ANALYST: own non-deleted only. ADMIN: any non-deleted. " +
                    "Returns 403 (not 404) for unauthorised access."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable UUID id) {

        User caller = securityContextHelper.getCurrentUser();
        TransactionResponse result = transactionService.getTransactionById(
                id, caller.getId(), caller.getRole());

        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(
            summary = "Update a transaction (partial)",
            description = "ANALYST may only update their own transactions. " +
                    "ADMIN may update any. Null fields in the body are ignored."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {

        User caller = securityContextHelper.getCurrentUser();
        TransactionResponse result = transactionService.updateTransaction(
                id, request, caller.getId(), caller.getRole());

        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(
            summary = "Soft-delete a transaction",
            description = "Sets is_deleted=true. Physical deletion never occurs. " +
                    "ANALYST may only soft-delete their own. ADMIN may delete any."
    )
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable UUID id) {
        User caller = securityContextHelper.getCurrentUser();
        transactionService.deleteTransaction(id, caller.getId(), caller.getRole());
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }
}
