package com.shreyash.zorvyn.finance_dashboard_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated user attempts an action they are not authorised for.
 * Always returns HTTP 403 Forbidden — never 404, to avoid leaking resource existence.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied: you do not have permission to perform this action");
    }
}
