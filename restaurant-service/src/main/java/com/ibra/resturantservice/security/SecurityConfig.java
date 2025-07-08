package com.ibra.resturantservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security features
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize and @PostAuthorize
public class SecurityConfig {

    // Define your custom filter as a Spring Bean
    @Bean
    public HeaderBasedAuthFilter headerBasedAuthFilter() {
        return new HeaderBasedAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless REST APIs using JWT/header-based auth
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Use stateless sessions for token-based security


                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints as per your requirement
                        .requestMatchers(HttpMethod.GET, "/api/restaurants").permitAll() // Get all active restaurants
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/{id}").permitAll() // Get restaurant by ID
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/search").permitAll() // Search restaurants by name/address
                        // The /admin/all endpoint (from your controller) requires ADMIN role, so it's not public.

                        // All other requests require authentication.
                        // The @PreAuthorize annotations on controller methods will then handle specific role checks.
                        .anyRequest().authenticated()
                )
                // Add your custom filter early in the chain.
                // It needs to run before any authorization decisions are made.
                // BasicAuthenticationFilter is a good anchor for early placement if no other
                // auth-related filters are expected to run before it.
                .addFilterBefore(headerBasedAuthFilter(), BasicAuthenticationFilter.class);

        // No formLogin() or httpBasic() needed as authentication is handled upstream by Auth Service/API Gateway.

        return http.build();
    }
}