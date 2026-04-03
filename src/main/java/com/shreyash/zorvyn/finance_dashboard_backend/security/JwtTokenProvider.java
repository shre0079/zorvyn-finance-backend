package com.shreyash.zorvyn.finance_dashboard_backend.security;

import com.shreyash.zorvyn.finance_dashboard_backend.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;


/**
 * Handles JWT creation, validation, and claims extraction.
 * Token claims:
 *  - sub  : userId (UUID as string)
 *  - email: user's email address
 *  - role : UserRole enum name (VIEWER / ANALYST / ADMIN)
 *  - iat  : issued-at (epoch seconds)
 *  - exp  : expiry    (epoch seconds)
 *
 * Algorithm: HS256 with a 256-bit (32-byte) base64-encoded secret.
 * The secret is injected from application.yml (app.jwt.secret).
 * Refresh tokens are not supported; re-login is required after expiry.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        this.signingKey  = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
    }

    // ── Token Generation ───────────────────────────────────────────────────

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userId UUID of the authenticated user
     * @param email  user's email address
     * @param role   user's role
     * @return compact JWT string
     */
    public String generateToken(UUID userId, String email, UserRole role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role",  role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ── Claims Extraction ─────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public UserRole extractRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Validates the token signature and expiry.
     * Does NOT verify that the userId still exists in the DB;
     * that check happens in CustomUserDetailsService.
     *
     * @throws JwtException if invalid
     * @throws ExpiredJwtException if expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token is expired: {}", ex.getMessage());
            throw ex;  // Re-throw so GlobalExceptionHandler maps it to 401
        } catch (JwtException ex) {
            log.warn("JWT token is invalid: {}", ex.getMessage());
            throw ex;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
