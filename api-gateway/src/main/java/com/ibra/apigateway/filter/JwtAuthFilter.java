package com.ibra.apigateway.filter;

import com.ibra.exception.BusinessException;
import com.ibra.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono; // Import Mono

import javax.crypto.SecretKey;
import java.util.List;
import java.util.function.Predicate;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    public static class Config {
        // Put the configuration properties
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            final List<String> openApiEndpoints = List.of(
                    "/auth/register",
                    "/auth/login",
                    "/auth/health",
                    "/api/restaurants",
                    "/api/restaurants/",
                    "/api/restaurants/search",
                    "/api/menu-items/restaurants/",
                    "/api/menu-items/"

            );

            // Check if the current request path is an open API endpoint
            Predicate<ServerHttpRequest> isSecured = r -> openApiEndpoints.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

            if (isSecured.test(request)) {
                // Check for Authorization header
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    logger.warn("Missing Authorization header for secured endpoint: {}", request.getURI().getPath());
                    return Mono.error(new UnauthorizedException("Missing Authorization header"));
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    logger.warn("Invalid Authorization header format for secured endpoint: {}", request.getURI().getPath());
                    return Mono.error(new UnauthorizedException("Invalid Authorization header format"));
                }

                String token = authHeader.substring(7); // Extract JWT token

                try {
                    // Validate JWT token
                    Claims claims = Jwts.parser()
                            .verifyWith(getKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    // Add user information to request headers for downstream services
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", claims.get("id", Long.class).toString())
                            .header("X-User-Email", claims.get("email", String.class))
                            .header("X-User-Role", claims.get("role", String.class))
                            .build();

                    logger.debug("JWT validated. Propagating headers for user ID: {} to {}", claims.get("id"), request.getURI().getPath());
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());

                } catch (ExpiredJwtException e) {
                    logger.warn("Expired JWT token for {}: {}", request.getURI().getPath(), e.getMessage());
                    // Throw ExpiredJwtException, which GatewayExceptionHandler will catch
                    return Mono.error(e);
                } catch (SignatureException e) {
                    logger.warn("Invalid JWT signature for {}: {}", request.getURI().getPath(), e.getMessage());
                    // Throw SignatureException, which GatewayExceptionHandler will catch
                    return Mono.error(e);
                } catch (Exception e) {
                    logger.error("Error validating JWT token for {}: {}", request.getURI().getPath(), e.getMessage(), e);
                    // For any other general parsing/validation errors, throw a BusinessException or generic RuntimeException
                    return Mono.error(new BusinessException("Invalid or malformed JWT token. Access denied."));
                }
            }
            // For unsecured endpoints, just pass the request through
            return chain.filter(exchange);
        };
    }
}
