package com.ibra.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibra.dto.ApiResponse;
import com.ibra.exception.BusinessException;
import com.ibra.exception.ResourceNotFoundException;
import com.ibra.exception.UnauthorizedException;
import io.jsonwebtoken.ExpiredJwtException; // For JWT expiration
import io.jsonwebtoken.security.SignatureException; // For JWT signature issues
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// @Order(-1) ensures this handler runs before Spring Boot's default error handler
@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // Set content type to JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "An unexpected error occurred at the API Gateway.";
        boolean success = false;

        // Custom handling for specific exceptions
        if (ex instanceof UnauthorizedException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = ex.getMessage();
            logger.warn("UnauthorizedException caught at Gateway: {}", ex.getMessage());
        } else if (ex instanceof ResourceNotFoundException) {
            httpStatus = HttpStatus.NOT_FOUND;
            errorMessage = ex.getMessage();
            logger.warn("ResourceNotFoundException caught at Gateway: {}", ex.getMessage());
        } else if (ex instanceof BusinessException) {
            httpStatus = HttpStatus.BAD_REQUEST;
            errorMessage = ex.getMessage();
            logger.warn("BusinessException caught at Gateway: {}", ex.getMessage());
        } else if (ex instanceof ExpiredJwtException) { // Specific JWT expiration
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = "JWT token has expired. Please log in again.";
            logger.warn("ExpiredJwtException caught at Gateway: {}", ex.getMessage());
        } else if (ex instanceof SignatureException) { // Specific JWT signature invalid
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = "Invalid JWT signature. Access denied.";
            logger.warn("SignatureException caught at Gateway: {}", ex.getMessage());
        } else if (ex instanceof IllegalArgumentException && ex.getMessage() != null &&
                (ex.getMessage().contains("JWT strings must contain exactly 2 periods") ||
                        ex.getMessage().contains("Invalid JWT format"))) {
            // Catch generic JWT format issues that might be thrown by JwtAuthFilter
            httpStatus = HttpStatus.UNAUTHORIZED;
            errorMessage = "Invalid JWT format. Access denied.";
            logger.warn("Invalid JWT format caught at Gateway: {}", ex.getMessage());
        }
        // Add more specific exception mappings as needed

        response.setStatusCode(httpStatus);

        // Create ApiResponse object
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(success)
                .message(errorMessage)
                .data(null)
                .build();

        // Convert ApiResponse to JSON and write to response body
        DataBufferFactory bufferFactory = response.bufferFactory();
        byte[] bytes = new byte[0];
        try {
            bytes = objectMapper.writeValueAsBytes(apiResponse);
        } catch (JsonProcessingException e) {
            logger.error("Error writing error response to JSON: {}", e.getMessage(), e);
            // Fallback to a simple error message if JSON serialization fails
            bytes = "{\"success\":false,\"message\":\"Internal server error during error handling\"}".getBytes();
        }

        return response.writeWith(Mono.just(bufferFactory.wrap(bytes)));
    }
}
