package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for PUT /api/users/{id} (ADMIN only).
 * All fields are optional — only non-null fields will be applied.
 * Role, fullName, and isActive are the only mutable fields (email is immutable).
 */


@Data
public class UpdateUserRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    /** If null, role is left unchanged. */
    private UserRole role;

    /** If null, isActive status is left unchanged. */
    private Boolean isActive;
}
