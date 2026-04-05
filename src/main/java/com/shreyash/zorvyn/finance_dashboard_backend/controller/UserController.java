package com.shreyash.zorvyn.finance_dashboard_backend.controller;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.*;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.AccessDeniedException;
import com.shreyash.zorvyn.finance_dashboard_backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management REST endpoints.
 * Role enforcement summary:
 *  GET /api/users           → ADMIN only
 *  GET /api/users/{id}      → ADMIN or the user themselves
 *  POST /api/users          → ADMIN only
 *  PUT /api/users/{id}      → ADMIN only
 *  DELETE /api/users/{id}   → ADMIN only
 *  PATCH /{id}/activate     → ADMIN only
 *  PATCH /{id}/deactivate   → ADMIN only
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "CRUD operations on user accounts (ADMIN only)")
public class UserController {

    private final UserService userService;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (paginated)", description = "ADMIN only.")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        PagedResponse<UserResponse> result = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userRepository.findById(#id).get().email")
    @Operation(
            summary = "Get user by ID",
            description = "ADMIN may retrieve any user. Non-ADMIN may only retrieve their own profile."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UUID   callerId   = securityContextHelper.getCurrentUserId();
        String callerRole = securityContextHelper.getCurrentUserRole().name();

        // If non-ADMIN, enforce ownership — service returns 403 on mismatch
        if (!"ADMIN".equals(callerRole) && !callerId.equals(id)) {
            throw new AccessDeniedException();
        }

        UserResponse result = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", result));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a user (ADMIN)", description = "ADMIN may assign any role.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse result = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a user (ADMIN)",
            description = "Partial update — only non-null fields are applied. Email is immutable."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse result = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Soft-deactivate a user (ADMIN)",
            description = "Sets isActive=false. Physical deletion never occurs."
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-activate a deactivated user (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", null));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }
}
