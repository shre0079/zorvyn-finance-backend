package com.shreyash.zorvyn.finance_dashboard_backend.security;

import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Loads user-specific data for Spring Security's authentication pipeline.
 *
 * The "username" in Spring Security's context is the user's email address.
 *
 * Roles are stored as "ROLE_<ROLENAME>" per Spring Security conventions,
 * enabling @PreAuthorize("hasRole('ADMIN')") expressions in controllers.
 *
 * Disabled accounts (isActive = false) are rejected at login with a
 * DisabledException (→ 401 ACCOUNT_DISABLED via GlobalExceptionHandler).
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("UserDetailsService: no user found for email '{}'", email);
                    return new UsernameNotFoundException(
                            "No account registered with email: " + email);
                });

        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("Login attempt for disabled account: '{}'", email);
            throw new DisabledException("Account is deactivated: " + email);
        }

        // Spring Security requires the ROLE_ prefix for hasRole() expressions
        String springRole = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(springRole)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)       // already checked above
                .build();
    }
}
