package com.shreyash.zorvyn.finance_dashboard_backend.exceptions;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for all REST endpoints.
 *
 * Every exception is converted to an {@link ErrorResponse} with the appropriate
 * HTTP status code. All responses follow the standard error envelope format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 Bad Request ────────────────────────────────────────────────────

    /**
     * Handles @Valid / @Validated failures on @RequestBody objects.
     * Returns a list of field-level errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed for request [{}]: {} field errors",
                request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Validation failed: " + fieldErrors.size() + " error(s)")
                        .errors(fieldErrors)
                        .errorCode("VALIDATION_ERROR")
                        .path(request.getRequestURI())
                        .build());
    }

    /**
     * Handles constraint violations on @RequestParam / @PathVariable.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(extractFieldName(cv))
                        .message(cv.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Constraint violation at [{}]", request.getRequestURI());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Constraint violation")
                        .errors(fieldErrors)
                        .errorCode("VALIDATION_ERROR")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.warn("Business validation error at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("VALIDATION_ERROR")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format(
                "Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue());

        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .message(message)
                        .errorCode("INVALID_PARAMETER")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── 401 Unauthorized ───────────────────────────────────────────────────

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {

        log.warn("Expired JWT token at [{}]", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("JWT token has expired. Please log in again.")
                        .errorCode("TOKEN_EXPIRED")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {

        log.warn("Invalid JWT at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Invalid or malformed JWT token.")
                        .errorCode("INVALID_TOKEN")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Invalid email or password.")
                        .errorCode("INVALID_CREDENTIALS")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(
            DisabledException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Your account has been deactivated. Please contact an administrator.")
                        .errorCode("ACCOUNT_DISABLED")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("ACCESS_DENIED")
                        .path(request.getRequestURI())
                        .build());
    }

    /**
     * Also catch Spring Security's own AccessDeniedException (different class)
     * so both produce the same 403 envelope.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Spring security access denied at [{}]", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("Access denied: you do not have permission to perform this action.")
                        .errorCode("ACCESS_DENIED")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.info("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("RESOURCE_NOT_FOUND")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.info("Conflict at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("RESOURCE_CONFLICT")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── 500 Internal Server Error (catch-all) ─────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .success(false)
                        .message("An unexpected error occurred. Please try again later.")
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .path(request.getRequestURI())
                        .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Extracts the simple field name from a ConstraintViolation property path.
     * e.g., "createTransaction.amount" → "amount"
     */
    private String extractFieldName(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
