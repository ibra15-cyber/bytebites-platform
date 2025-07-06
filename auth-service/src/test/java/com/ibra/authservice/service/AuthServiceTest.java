package com.ibra.authservice.service;
import com.ibra.authservice.dto.AuthResponse;
import com.ibra.authservice.dto.LoginRequest;
import com.ibra.authservice.dto.RegisterRequest;
import com.ibra.authservice.entity.User;
import com.ibra.authservice.enums.UserRole;
import com.ibra.authservice.repository.UserRepository;
import com.ibra.authservice.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(UserRole.CUSTOMER);
        mockUser.setCreatedAt(LocalDateTime.now());

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.CUSTOMER);

        loginRequest = new LoginRequest("john@example.com", "password123");
    }

    // ===== REGISTRATION TESTS =====

    @Test
    void registerUser_NewEmail_SuccessfulRegistration() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        Optional<Map<String, String>> result = authService.registerUser(registerRequest);

        assertTrue(result.isEmpty());
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ExistingEmail_ReturnsError() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        Optional<Map<String, String>> result = authService.registerUser(registerRequest);

        assertTrue(result.isPresent());
        assertEquals("Email is already taken!", result.get().get("error"));
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_PasswordEncodingCalled() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

        authService.registerUser(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    // ===== LOGIN TESTS =====

    @Test
    void loginUser_ValidCredentials_ReturnsAuthResponse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        when(jwtUtils.generateToken(mockUser)).thenReturn("mock-jwt-token");

        AuthResponse result = authService.loginUser(loginRequest);

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("Bearer", result.getType());
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(UserRole.CUSTOMER, result.getRole());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateToken(mockUser);
    }

    @Test
    void loginUser_InvalidCredentials_ThrowsAuthenticationException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("Invalid credentials") {});

        assertThrows(AuthenticationException.class, () -> {
            authService.loginUser(loginRequest);
        });
    }

    // ===== PROFILE TESTS =====

    @Test
    void getUserProfile_ExistingUser_ReturnsProfile() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        Optional<Map<String, Object>> result = authService.getUserProfile("john@example.com");

        assertTrue(result.isPresent());
        Map<String, Object> profile = result.get();
        assertEquals(1L, profile.get("id"));
        assertEquals("John", profile.get("firstName"));
        assertEquals("Doe", profile.get("lastName"));
        assertEquals("john@example.com", profile.get("email"));
        assertEquals(UserRole.CUSTOMER, profile.get("role"));
        assertNotNull(profile.get("createdAt"));
    }

    @Test
    void getUserProfile_NonExistingUser_ReturnsEmpty() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<Map<String, Object>> result = authService.getUserProfile("nonexistent@example.com");

        assertTrue(result.isEmpty());
    }
}