package com.ibra.security.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibra.dto.ApiResponse;
import com.ibra.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * Custom AuthenticationEntryPoint to handle unauthenticated requests.
 * This is invoked when a user tries to access a secured REST endpoint
 * without proper authentication (e.g., missing/invalid JWT).
 * It returns a JSON ApiResponse with HttpStatus.UNAUTHORIZED.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication required or token is invalid: " + authException.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI() // Populate path
        );

        try (OutputStream out = response.getOutputStream()) {
            objectMapper.writeValue(out, errorResponse);
        } catch (Exception e) {
            logger.error("Error writing unauthorized response: {}", e.getMessage(), e);
        }
    }
}

