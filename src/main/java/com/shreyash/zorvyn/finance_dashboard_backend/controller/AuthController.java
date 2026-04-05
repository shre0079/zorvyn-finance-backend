package com.shreyash.zorvyn.finance_dashboard_backend.controller;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.ApiResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.AuthResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.request.LoginRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.request.RegisterRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints.
 * These endpoints are excluded from JWT authentication via SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and log in to obtain a JWT token")
@SecurityRequirements   // Override global Bearer requirement — these endpoints are public
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Self-register a new account",
            description = "Creates a new user with the VIEWER role. " +
                    "Role field in the request is ignored; only ADMIN can assign roles."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in and obtain a JWT token",
            description = "Authenticate with email + password. Returns a signed JWT " +
                    "valid for 24 hours (configurable via app.jwt.expiration-ms)."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
