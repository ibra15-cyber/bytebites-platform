package com.ibra.security.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibra.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Custom AccessDeniedHandler to handle authenticated requests where the user
 * does not have the necessary permissions/roles to access a resource.
 * It returns a JSON ApiResponse with HttpStatus.FORBIDDEN.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        logger.error("Access Denied error: {}", accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message("Access Denied: You do not have sufficient permissions to access this resource.")
                .data(null)
                .build();

        try (OutputStream out = response.getOutputStream()) {
            objectMapper.writeValue(out, apiResponse);
        } catch (Exception e) {
            logger.error("Error writing access denied response: {}", e.getMessage(), e);
        }
    }
}

