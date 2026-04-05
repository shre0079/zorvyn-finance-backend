package com.shreyash.zorvyn.finance_dashboard_backend.controller;

import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Convenience helper to extract the authenticated caller's User entity and role
 * from the current Spring Security context.
 * Controllers use this to obtain the caller's UUID and UserRole without
 * needing to inject UserRepository themselves.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UserRepository userRepository;

    /** Returns the fully-loaded User entity for the currently authenticated caller. */
    public User getCurrentUser() {
        String email = getCurrentEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB: " + email));
    }

    /** Returns the UUID of the currently authenticated caller. */
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /** Returns the UserRole of the currently authenticated caller. */
    public UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /** Returns the email (Spring Security "username") of the authenticated caller. */
    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return principal.toString();
    }
}
