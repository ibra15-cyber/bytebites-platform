package com.ibra.resturantservice.security;

import com.ibra.security.filter.HeaderBasedAuthFilter;
import com.ibra.security.handler.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security features
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize and @PostAuthorize
public class SecurityConfig {

    // Define your custom filter as a Spring Bean
    private final HeaderBasedAuthFilter headerBasedAuthFilter;
    private final AccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(HeaderBasedAuthFilter headerBasedAuthFilter, AccessDeniedHandler jwtAccessDeniedHandler, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.headerBasedAuthFilter = headerBasedAuthFilter;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless REST APIs using JWT/header-based auth
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Use stateless sessions for token-based security


                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/restaurants").permitAll() // Get all active restaurants
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/{id}").permitAll() // Get restaurant by ID
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/search").permitAll() // Search restaurants by name/address
                         .anyRequest().authenticated()
                )
                .addFilterBefore(headerBasedAuthFilter, BasicAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // For unauthenticated access
                        .accessDeniedHandler(jwtAccessDeniedHandler) // For authenticated but unauthorized access
                );
        return http.build();
    }
}