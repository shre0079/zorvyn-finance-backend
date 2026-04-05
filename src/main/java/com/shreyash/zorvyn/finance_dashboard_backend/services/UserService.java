package com.shreyash.zorvyn.finance_dashboard_backend.services;


import com.shreyash.zorvyn.finance_dashboard_backend.dtos.CreateUserRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.PagedResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.UpdateUserRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.UserResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.AccessDeniedException;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.DuplicateResourceException;
import com.shreyash.zorvyn.finance_dashboard_backend.exceptions.ResourceNotFoundException;
import com.shreyash.zorvyn.finance_dashboard_backend.mapper.UserMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User management operations (ADMIN-only, except GET own profile).
 * All "not found" cases return ResourceNotFoundException (404).
 * Ownership checks return AccessDeniedException (403) — never 404 —
 * to avoid leaking resource existence to lower-privileged callers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * Paginated list of all users (ADMIN only — enforced at controller level).
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));

        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.from(userPage, content);
    }

    /**
     * Retrieve a single user by ID.
     * ADMIN may retrieve any user.
     * Non-ADMIN callers may only retrieve their own profile
     * (enforced at the controller via @PreAuthorize; this service
     *  accepts any valid UUID after that check passes).
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    // ── Create ────────────────────────────────────────────────────────────

    /**
     * ADMIN creates a user with an explicitly-assigned role.
     * Throws 409 if email is already in use.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        log.info("ADMIN created user: {} ({})", saved.getEmail(), saved.getRole());
        return userMapper.toResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * ADMIN updates fullName, role, and/or isActive for any user.
     * Null fields in the request are ignored (partial update via MapStruct).
     */
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        userMapper.partialUpdate(user, request);
        User saved = userRepository.save(user);
        log.info("User {} updated by ADMIN", id);
        return userMapper.toResponse(saved);
    }

    // ── Soft deactivate / activate ────────────────────────────────────────

    /**
     * Soft-deactivates a user (sets isActive = false).
     * Does not physically delete the record or cascade-delete transactions.
     */
    @Transactional
    public void deactivateUser(UUID id) {
        ensureUserExists(id);
        int updated = userRepository.updateIsActiveById(id, false);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        log.info("User {} deactivated", id);
    }

    /**
     * Re-activates a previously deactivated user.
     */
    @Transactional
    public void activateUser(UUID id) {
        ensureUserExists(id);
        int updated = userRepository.updateIsActiveById(id, true);
        if (updated == 0) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        log.info("User {} activated", id);
    }

    /**
     * Soft-delete via the DELETE endpoint — same as deactivate.
     * Physical deletion never occurs.
     */
    @Transactional
    public void deleteUser(UUID id) {
        deactivateUser(id);
    }

    // ── Ownership verification (used by other services) ───────────────────

    /**
     * Returns the User entity for the given ID.
     * Throws 403 (not 404) if the callerUserId does not match the targetId
     * and the caller is not ADMIN.
     *
     * This is called by services when a non-ADMIN tries to access another
     * user's resource — we return 403, not 404, so callers cannot probe
     * for the existence of other users' resources.
     */
    public User getOwnUserOrThrow(UUID targetId, UUID callerUserId) {
        User user = findUserOrThrow(targetId);
        if (!user.getId().equals(callerUserId)) {
            throw new AccessDeniedException();
        }
        return user;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    public User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private void ensureUserExists(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
    }
}
