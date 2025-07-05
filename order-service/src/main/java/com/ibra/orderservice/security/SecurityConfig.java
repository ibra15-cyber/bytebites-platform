package com.ibra.orderservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public HeaderBasedAuthFilter headerBasedAuthFilter() {
        return new HeaderBasedAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                                // If all /api/orders/** endpoints require authentication
                                // and their specific roles are handled by @PreAuthorize on controller methods,
                                // then you can simply state that any request needs to be authenticated.
                                .anyRequest().authenticated()
                        // If you *do* have public endpoints for the Order Service (e.g., a health check specific to orders)
                        // you would add them here. For example:
                        // .requestMatchers(HttpMethod.GET, "/api/orders/health").permitAll()
                )
                .addFilterBefore(headerBasedAuthFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}