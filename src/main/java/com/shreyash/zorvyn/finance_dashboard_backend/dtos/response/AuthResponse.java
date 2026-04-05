package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;

import lombok.Builder;
import lombok.Data;

/**
 * Returned by POST /api/auth/login and POST /api/auth/register.
 *
 * {
 *   "token": "eyJhbGci...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400000,
 *   "user": { ... }
 * }
 */

@Data
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Milliseconds until token expiry (mirrors app.jwt.expiration-ms). */
    private long expiresIn;

    private UserResponse user;
}
