package com.shreyash.zorvyn.finance_dashboard_backend.repositories;

import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Used by login and duplicate-email checks. */
    Optional<User> findByEmail(String email);

    /** Used by CustomUserDetailsService and duplicate-email validation. */
    boolean existsByEmail(String email);

    /** Paginated list for GET /api/users (ADMIN only). */
    Page<User> findAll(Pageable pageable);

    /** Find active users by role (utility query). */
    Page<User> findByRoleAndIsActive(UserRole role, Boolean isActive, Pageable pageable);

    /**
     * Soft-deactivate without loading the entity.
     * Preferred over loading + saving to avoid unnecessary SELECT.
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id = :id")
    int updateIsActiveById(@Param("id") UUID id, @Param("active") boolean active);
}
