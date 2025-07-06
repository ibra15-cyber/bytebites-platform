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
        String userRolesHeader = request.getHeader("X-User-Role");
        String email = request.getHeader("X-User-Email");

        if (userId != null && !userId.isEmpty() && userRolesHeader != null && !userRolesHeader.isEmpty()) {
            try {
                Long parsedUserId = Long.parseLong(userId);
                List<SimpleGrantedAuthority> authorities = Arrays.stream(userRolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(parsedUserId, null, authorities);
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
        filterChain.doFilter(request, response);
    }
}