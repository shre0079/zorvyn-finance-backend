package com.shreyash.zorvyn.finance_dashboard_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every request exactly once (extends {@link OncePerRequestFilter}).
 *
 * Workflow:
 *  1. Extract "Bearer <token>" from the Authorization header.
 *  2. Validate the token via JwtTokenProvider.
 *  3. Load UserDetails via CustomUserDetailsService.
 *  4. Set the Authentication in the SecurityContext.
 *
 * If any step fails, the filter writes a JSON ErrorResponse directly to the
 * response and short-circuits the chain (no further processing).
 * If no Authorization header is present the filter passes through normally
 * (unauthenticated requests will be rejected by Spring Security later).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token == null) {
            // No token present — continue and let Spring Security decide
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtTokenProvider.validateToken(token);

            String email = jwtTokenProvider.extractEmail(token);

            // Only set authentication if not already authenticated
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user '{}' for request [{}]",
                        email, request.getRequestURI());
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT for request [{}]", request.getRequestURI());
            writeErrorResponse(response, request.getRequestURI(),
                    HttpStatus.UNAUTHORIZED, "JWT token has expired. Please log in again.",
                    "TOKEN_EXPIRED");

        } catch (JwtException ex) {
            log.warn("Invalid JWT for request [{}]: {}", request.getRequestURI(), ex.getMessage());
            writeErrorResponse(response, request.getRequestURI(),
                    HttpStatus.UNAUTHORIZED, "Invalid or malformed JWT token.",
                    "INVALID_TOKEN");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Extracts the raw JWT string from the Authorization header.
     * Returns null if the header is absent or not a Bearer token.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Writes a JSON {@link ErrorResponse} directly to the servlet response
     * without going through Spring MVC dispatcher.
     */
    private void writeErrorResponse(
            HttpServletResponse response,
            String path,
            HttpStatus status,
            String message,
            String errorCode) throws IOException {

        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}