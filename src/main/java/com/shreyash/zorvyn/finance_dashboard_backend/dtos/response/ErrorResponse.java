package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Universal error-response envelope.
 *
 * Example (validation error):
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "errors": [
 *     { "field": "email", "message": "Must be a valid email address" }
 *   ],
 *   "errorCode": "VALIDATION_ERROR",
 *   "timestamp": "2024-04-01T10:30:00Z",
 *   "path": "/api/auth/register"
 * }
 */


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String message;

    /** Populated only for 400 validation errors. */
    private List<FieldError> errors;

    private String errorCode;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String path;

    // Nested FieldError

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}