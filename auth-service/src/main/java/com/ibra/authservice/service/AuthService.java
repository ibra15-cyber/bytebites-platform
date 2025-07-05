package com.ibra.authservice.service;

import com.ibra.authservice.dto.AuthResponse;
import com.ibra.authservice.dto.LoginRequest;
import com.ibra.authservice.dto.RegisterRequest;
import com.ibra.authservice.entity.User;
import com.ibra.authservice.repository.UserRepository;
import com.ibra.authservice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtil;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<Map<String, String>> registerUser(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email is already taken!");
            return Optional.of(error);
        }

        // Create new user
        User user = new User(
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getRole()
        );

        userRepository.save(user);
        return Optional.empty(); // No error, registration successful
    }

    public AuthResponse loginUser(LoginRequest loginRequest) throws AuthenticationException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal(); // Assuming User entity implements UserDetails
        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    public Optional<Map<String, Object>> getUserProfile(String userEmail) {
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("firstName", user.getFirstName());
            profile.put("lastName", user.getLastName());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole());
            profile.put("createdAt", user.getCreatedAt());
            return Optional.of(profile);
        }
        return Optional.empty();
    }
}
