package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public-facing representation of a User entity.
 * passwordHash is intentionally excluded — never exposed via API.
 */

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}