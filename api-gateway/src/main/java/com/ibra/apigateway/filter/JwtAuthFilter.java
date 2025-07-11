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
import reactor.core.publisher.Mono;

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
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            // Define truly public endpoints, specifying method where necessary
            // These endpoints will bypass JWT validation
            final List<String> PUBLIC_GET_ENDPOINTS = List.of(
                    "/api/restaurants",             // GET /api/restaurants (get all active)
                    "/api/restaurants/search",      // GET /api/restaurants/search
                    "/api/restaurants/owner/",      // GET /api/restaurants/owner/{ownerId}
                    "/api/menu-items/restaurants/", // GET /api/menu-items/restaurants/{restaurantId}/...
                    "/api/menu-items/"              // GET /api/menu-items/{id} or /api/menu-items (if applicable)
            );

            final List<String> PUBLIC_POST_ENDPOINTS = List.of(
                    "/auth/register",
                    "/auth/login",
                    "/auth/health"
            );

            // Predicate to determine if a request is public and should bypass JWT validation
            Predicate<ServerHttpRequest> isPublic = r -> {
                String currentPath = r.getURI().getPath();
                String currentMethod = r.getMethod().name();

                // Check for public GET endpoints
                if ("GET".equals(currentMethod)) {
                    if (PUBLIC_GET_ENDPOINTS.stream().anyMatch(publicPath -> currentPath.startsWith(publicPath))) {
                        return true;
                    }
                }
                // Check for public POST endpoints
                else if ("POST".equals(currentMethod)) {
                    if (PUBLIC_POST_ENDPOINTS.stream().anyMatch(publicPath -> currentPath.startsWith(publicPath))) {
                        return true;
                    }
                }
                return false;
            };


            if (isPublic.test(request)) {
                logger.debug("Bypassing JWT validation for public endpoint: {} {}", method, path);
                return chain.filter(exchange);
            }

            // If the request is not public, it must be secured. Proceed with JWT validation.
            // Check for Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("Missing Authorization header for secured endpoint: {} {}", method, path);
                return Mono.error(new UnauthorizedException("Missing Authorization header"));
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header format for secured endpoint: {} {}", method, path);
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

                logger.debug("JWT validated. Propagating headers for user ID: {} to {}", claims.get("id"), path);
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (ExpiredJwtException e) {
                logger.warn("Expired JWT token for {}: {}", path, e.getMessage());
                return Mono.error(e); // Let GlobalExceptionHandler handle this
            } catch (SignatureException e) {
                logger.warn("Invalid JWT signature for {}: {}", path, e.getMessage());
                return Mono.error(e); // Let GlobalExceptionHandler handle this
            } catch (Exception e) {
                logger.error("Error validating JWT token for {}: {}", path, e.getMessage(), e);
                return Mono.error(new BusinessException("Invalid or malformed JWT token. Access denied."));
            }
        };
    }
}
