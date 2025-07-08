package com.ibra.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component; // Make it a Spring Component
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.util.annotation.NonNull; // Keep if you use reactor annotations, otherwise remove

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HeaderBasedAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderBasedAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userRolesHeader = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email"); // Assuming this header is also propagated

        if (userId != null && !userId.isEmpty() && userRolesHeader != null && !userRolesHeader.isEmpty()) {
            try {
                Long parsedUserId = Long.parseLong(userId);
                List<SimpleGrantedAuthority> authorities = Arrays.stream(userRolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new) // Assumes roles like "ROLE_ADMIN" or "ADMIN" if your @PreAuthorize uses hasAuthority
                        .collect(Collectors.toList());

                // The principal can be any object representing the user. Here, we'll use the userId.
                // Credentials are null as authentication happened upstream at the Gateway.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(parsedUserId, null, authorities);
                // Optionally, you can add more details to the principal if needed, e.g., the email
                // authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Populated SecurityContext with user ID: {} and roles: {}, and email: {}", parsedUserId, userRolesHeader, userEmail);

            } catch (NumberFormatException e) {
                logger.warn("Invalid X-User-Id header format: {}", userId);
            } catch (Exception e) {
                logger.error("Error processing header-based authentication: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No X-User-Id or X-User-Roles headers found. Proceeding without explicit authentication for this request.");
        }
        filterChain.doFilter(request, response);
    }
}
