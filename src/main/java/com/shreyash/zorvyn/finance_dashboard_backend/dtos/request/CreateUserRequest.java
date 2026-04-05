package com.shreyash.zorvyn.finance_dashboard_backend.dtos.request;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;


/**
 * Payload for POST /api/users (ADMIN only).
 * Allows an administrator to create a user with an explicit role assignment.
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter and one digit"
    )
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private UserRole role;
}
