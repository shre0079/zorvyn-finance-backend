package com.shreyash.zorvyn.finance_dashboard_backend.services;

import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.AuthResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.request.LoginRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.request.RegisterRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.UserResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.DuplicateResourceException;
import com.shreyash.zorvyn.finance_dashboard_backend.mapper.UserMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.UserRepository;
import com.shreyash.zorvyn.finance_dashboard_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and login.
 * Design decisions:
 *  1. Self-registration always creates a VIEWER, regardless of what role is passed.
 *     Only ADMIN-authenticated callers (via UserService.createUser) may assign other roles.
 *  2. Re-registering an existing email returns 409 Conflict (not a JWT).
 *  3. Login delegates to Spring Security's AuthenticationManager which uses
 *     CustomUserDetailsService + BCrypt comparison internally.
 *  4. JWT does not support refresh; re-login is required after expiry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // ── Registration ──────────────────────────────────────────────────────

    /**
     * Self-registration endpoint. Role is always forced to VIEWER here.
     * Throws 409 if the email is already registered.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Self-registration is always VIEWER; role in request is ignored
        user.setRole(UserRole.VIEWER);

        User saved = userRepository.save(user);
        log.info("New user registered: {} ({})", saved.getEmail(), saved.getRole());

        String token = jwtTokenProvider.generateToken(
                saved.getId(), saved.getEmail(), saved.getRole());

        return buildAuthResponse(token, saved);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    /**
     * Authenticates the user via Spring Security's AuthenticationManager.
     * Throws BadCredentialsException (→ 401) on wrong credentials.
     * Throws DisabledException (→ 401) if the account is deactivated.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // This triggers CustomUserDetailsService.loadUserByUsername() + BCrypt check
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Re-load the entity to get the full User (including UUID and role)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "User disappeared after successful authentication: "
                                + request.getEmail()));

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole());

        log.info("User logged in: {} ({})", user.getEmail(), user.getRole());
        return buildAuthResponse(token, user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(userResponse)
                .build();
    }
}
