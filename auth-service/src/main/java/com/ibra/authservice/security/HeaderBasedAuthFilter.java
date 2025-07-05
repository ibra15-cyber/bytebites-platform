package com.ibra.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.util.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class HeaderBasedAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderBasedAuthFilter.class);

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id"); //
        String userEmail = request.getHeader("X-User-Email"); //
        String userRoleHeader = request.getHeader("X-User-Role"); //

        if (userId != null && !userId.isEmpty() && userEmail != null && !userEmail.isEmpty()) {
            try {
                // Parse userId to Long if your service's User entity uses Long IDs for ID from JWT subject
                // If the JWT subject is a String ID, keep userId as String in principal.
                Long parsedUserId = Long.parseLong(userId); // Assuming userId is a parsable Long

                List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                if (userRoleHeader != null && !userRoleHeader.isEmpty()) {
                    // Assuming X-User-Role header contains a single role string like "ADMIN" or "RESTAURANT_OWNER"
                    // If it contains comma-separated roles, adjust split logic.
                    // The Gateway's JwtAuthFilter extracts 'role' as a single string
                    authorities = Collections.singletonList(new SimpleGrantedAuthority(userRoleHeader));
                }

                // Create an Authentication object.
                // The principal can be any object representing the user. Here, we'll use userId.
                // Credentials are null as authentication happened upstream at the Gateway.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(parsedUserId, null, authorities);

                // Set the Authentication object in the SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Populated SecurityContext for user ID: {} with role: {}", parsedUserId, userRoleHeader);

            } catch (NumberFormatException e) {
                logger.warn("Invalid X-User-Id header format received: {}", userId, e);
            } catch (Exception e) {
                logger.error("Error processing header-based authentication: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("X-User-Id or X-User-Email/Role headers are missing or empty. Proceeding without authentication for this request.");
        }

        filterChain.doFilter(request, response);
    }
}