package com.shreyash.zorvyn.finance_dashboard_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (user, transaction) does not exist.
 * Maps to HTTP 404 Not Found.
 * NOTE: For access-control violations, AccessDeniedException (403) is thrown
 * instead of this — callers must never learn of a resource's existence via a 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
