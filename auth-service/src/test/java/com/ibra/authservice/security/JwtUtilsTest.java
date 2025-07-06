package com.ibra.authservice.security;

import static org.junit.jupiter.api.Assertions.*;


// ===== SECURITY TESTS =====

import com.ibra.authservice.entity.User;
import com.ibra.authservice.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private User testUser;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Set private fields using reflection for testing
        setField(jwtUtils, "jwtSecret", jwtSecret);
        setField(jwtUtils, "jwtExpirationMs", jwtExpiration);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole(UserRole.CUSTOMER);
    }

    @Test
    void generateToken_ValidUser_GeneratesValidToken() {
        String token = jwtUtils.generateToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);

        // Verify token structure (JWT has 3 parts separated by dots)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void extractEmail_ValidToken_ReturnsCorrectEmail() {
        String token = jwtUtils.generateToken(testUser);

        String extractedEmail = jwtUtils.extractEmail(token);

        assertEquals("john@example.com", extractedEmail);
    }

    @Test
    void extractAllClaims_ValidToken_ReturnsCorrectClaims() {
        String token = jwtUtils.generateToken(testUser);

        Claims claims = jwtUtils.extractAllClaims(token);

        assertEquals("john@example.com", claims.getSubject());
        assertEquals(1L, claims.get("id", Long.class));
        assertEquals("John", claims.get("firstName"));
        assertEquals("Doe", claims.get("lastName"));
        assertEquals("CUSTOMER", claims.get("role"));
        assertEquals("john@example.com", claims.get("email"));
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtUtils.generateToken(testUser);

        Boolean isValid = jwtUtils.validateToken(token, "john@example.com");

        assertTrue(isValid);
    }

    @Test
    void validateToken_WrongEmail_ReturnsFalse() {
        String token = jwtUtils.generateToken(testUser);

        Boolean isValid = jwtUtils.validateToken(token, "wrong@example.com");

        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = jwtUtils.generateToken(testUser);

        Boolean isExpired = jwtUtils.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void extractExpiration_ValidToken_ReturnsExpirationDate() {
        String token = jwtUtils.generateToken(testUser);

        Date expiration = jwtUtils.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    // Helper method to set private fields using reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

}
