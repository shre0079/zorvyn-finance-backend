package com.shreyash.zorvyn.finance_dashboard_backend.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Universal response envelope.
 *
 * All controllers return ApiResponse<T>; the HTTP status code is set
 * independently via ResponseEntity.
 *
 * Example:
 * {
 *   "success": true,
 *   "message": "Transaction retrieved successfully",
 *   "data": { ... },
 *   "timestamp": "2024-04-01T10:30:00Z"
 * }
 */

 @Data
 @Builder
 @JsonInclude(JsonInclude.Include.NON_NULL)
 public class ApiResponse<T> {

 @Builder.Default
 private boolean success = true;

 private String message;

 private T data;

 @Builder.Default
 private String timestamp = Instant.now().toString();

 // Factory helpers

 public static <T> ApiResponse<T> success(String message, T data) {
 return ApiResponse.<T>builder()
 .success(true)
 .message(message)
 .data(data)
 .build();
 }

 public static <T> ApiResponse<T> success(T data) {
 return success("Operation completed successfully", data);
 }
 }