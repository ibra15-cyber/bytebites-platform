package com.ibra.orderservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.util.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderBasedAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderBasedAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRolesHeader = request.getHeader("X-User-Role"); // e.g., "ROLE_ADMIN,ROLE_RESTAURANT_OWNER"
        String email = request.getHeader("X-User-Email");

        if (userId != null && !userId.isEmpty() && userRolesHeader != null && !userRolesHeader.isEmpty()) {
            try {
                Long parsedUserId = Long.parseLong(userId); // Parse userId to Long if your service uses Long IDs
                List<SimpleGrantedAuthority> authorities = Arrays.stream(userRolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        // Ensure roles are in the correct format for Spring Security (e.g., "ROLE_ADMIN")
                        // If your X-User-Roles already has "ROLE_", then remove `role -> "ROLE_" + role`
                        // Make sure it matches what @PreAuthorize expects.
                        .map(SimpleGrantedAuthority::new) // If roles are already like "ROLE_ADMIN"
                        // .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // If roles are like "ADMIN"
                        .collect(Collectors.toList());

                // Create an Authentication object.
                // The principal can be any object representing the user. Here, we'll use the userId.
                // Credentials are null as authentication happened upstream.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(parsedUserId, null, authorities);

                // Set the Authentication object in the SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Populated SecurityContext with user ID: {} and roles: {}, and email: {}", parsedUserId, userRolesHeader, email);

            } catch (NumberFormatException e) {
                logger.warn("Invalid X-User-Id header format: {}", userId);
            } catch (Exception e) {
                logger.error("Error processing header-based authentication: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No X-User-Id or X-User-Roles headers found. Proceeding without explicit authentication for this request.");
        }

        // Continue the filter chain. Spring Security's authorizeHttpRequests and @PreAuthorize
        // will then evaluate based on the populated SecurityContext (or lack thereof).
        filterChain.doFilter(request, response);
    }
}