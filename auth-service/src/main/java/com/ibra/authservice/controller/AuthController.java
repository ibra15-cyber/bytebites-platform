package com.ibra.authservice.controller;

import com.ibra.authservice.dto.AuthResponse;
import com.ibra.authservice.dto.LoginRequest;
import com.ibra.authservice.dto.RegisterRequest;
import com.ibra.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // Consider restricting origins in production
public class AuthController {

    private final AuthService authService; // Inject the new service

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        Optional<Map<String, String>> error = authService.registerUser(registerRequest);
        if (error.isPresent()) {
            return ResponseEntity.badRequest().body(error.get());
        }

        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", "Registration successful. Please log in.");
        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.loginUser(loginRequest);
            return ResponseEntity.ok(authResponse);

        } catch (AuthenticationException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("X-User-Id") String userId,
                                            @RequestHeader("X-User-Email") String userEmail) {
        Optional<Map<String, Object>> profile = authService.getUserProfile(userEmail);

        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User profile not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "auth-service");
        return ResponseEntity.ok(status);
    }
}
