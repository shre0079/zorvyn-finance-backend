package com.shreyash.zorvyn.finance_dashboard_backend.dtos.request;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * Password rules:
     *  - Minimum 8 characters
     *  - At least one uppercase letter
     *  - At least one digit
     * Validated via @Pattern
     */
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

    /**
     * Role is optional on self-registration.
     * - If not supplied, defaults to VIEWER in AuthService.
     * - If supplied by a non-ADMIN caller, the service ignores it and forces VIEWER.
     * - Only ADMIN callers may successfully set ANALYST or ADMIN roles here.
     */
    private UserRole role;
}
