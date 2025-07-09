//package com.ibra.authservice.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.ibra.authservice.dto.AuthResponse;
//import com.ibra.authservice.dto.LoginRequest;
//import com.ibra.authservice.dto.RegisterRequest;
//import com.ibra.authservice.enums.UserRole;
//import com.ibra.authservice.controller.AuthController;
//import com.ibra.authservice.service.AuthService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private AuthService authService;
//
//    @InjectMocks
//    private AuthController authController;
//
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//    private RegisterRequest validRegisterRequest;
//    private LoginRequest validLoginRequest;
//    private AuthResponse mockAuthResponse;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(authController)
//                .defaultRequest(post("/").with(csrf()))
//                .build();
//
//        validRegisterRequest = new RegisterRequest();
//        validRegisterRequest.setFirstName("John");
//        validRegisterRequest.setLastName("Doe");
//        validRegisterRequest.setEmail("john@example.com");
//        validRegisterRequest.setPassword("password123");
//        validRegisterRequest.setRole(UserRole.CUSTOMER);
//
//        validLoginRequest = new LoginRequest("john@example.com", "password123");
//
//        mockAuthResponse = new AuthResponse(
//                "mock-jwt-token",
//                1L,
//                "john@example.com",
//                "John",
//                "Doe",
//                UserRole.CUSTOMER
//        );
//    }
//
//    // ===== REGISTRATION TESTS =====
//
//    @Test
//    void registerUser_ValidRequest_ReturnsSuccess() throws Exception {
//        when(authService.registerUser(any(RegisterRequest.class)))
//                .thenReturn(Optional.empty());
//
//        mockMvc.perform(post("/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("Registration successful. Please log in."));
//    }
//
//    @Test
//    void registerUser_EmailAlreadyExists_ReturnsBadRequest() throws Exception {
//        Map<String, String> error = new HashMap<>();
//        error.put("error", "Email is already taken!");
//
//        when(authService.registerUser(any(RegisterRequest.class)))
//                .thenReturn(Optional.of(error));
//
//        mockMvc.perform(post("/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Email is already taken!"));
//    }
//
//    @Test
//    void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception {
//        validRegisterRequest.setEmail("invalid-email");
//
//        mockMvc.perform(post("/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void registerUser_MissingFields_ReturnsBadRequest() throws Exception {
//        RegisterRequest invalidRequest = new RegisterRequest();
//        invalidRequest.setEmail("test@example.com");
//
//        mockMvc.perform(post("/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void registerUser_ShortPassword_ReturnsBadRequest() throws Exception {
//        validRegisterRequest.setPassword("123");
//
//        mockMvc.perform(post("/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // ===== LOGIN TESTS =====
//
//    @Test
//    void loginUser_ValidCredentials_ReturnsAuthResponse() throws Exception {
//        when(authService.loginUser(any(LoginRequest.class)))
//                .thenReturn(mockAuthResponse);
//
//        mockMvc.perform(post("/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validLoginRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.email").value("john@example.com"))
//                .andExpect(jsonPath("$.firstName").value("John"))
//                .andExpect(jsonPath("$.lastName").value("Doe"))
//                .andExpect(jsonPath("$.role").value("CUSTOMER"))
//                .andExpect(jsonPath("$.type").value("Bearer"));
//    }
//
//    @Test
//    void loginUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
//        when(authService.loginUser(any(LoginRequest.class)))
//                .thenThrow(new AuthenticationException("Invalid credentials") {});
//
//        mockMvc.perform(post("/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validLoginRequest)))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.error").value("Invalid email or password"));
//    }
//
//    @Test
//    void loginUser_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
//        LoginRequest invalidLogin = new LoginRequest("invalid-email", "password");
//
//        mockMvc.perform(post("/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidLogin)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void loginUser_MissingPassword_ReturnsBadRequest() throws Exception {
//        LoginRequest invalidLogin = new LoginRequest("john@example.com", "");
//
//        mockMvc.perform(post("/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invalidLogin)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // ===== PROFILE TESTS =====
//
//    @Test
//    @WithMockUser
//    void getUserProfile_ValidHeaders_ReturnsProfile() throws Exception {
//        Map<String, Object> profile = new HashMap<>();
//        profile.put("id", 1L);
//        profile.put("firstName", "John");
//        profile.put("lastName", "Doe");
//        profile.put("email", "john@example.com");
//        profile.put("role", UserRole.CUSTOMER);
//
//        when(authService.getUserProfile(anyString()))
//                .thenReturn(Optional.of(profile));
//
//        mockMvc.perform(get("/auth/profile")
//                        .header("X-User-Id", "1")
//                        .header("X-User-Email", "john@example.com"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.firstName").value("John"))
//                .andExpect(jsonPath("$.lastName").value("Doe"))
//                .andExpect(jsonPath("$.email").value("john@example.com"))
//                .andExpect(jsonPath("$.role").value("CUSTOMER"));
//    }
//
//    @Test
//    @WithMockUser
//    void getUserProfile_UserNotFound_ReturnsNotFound() throws Exception {
//        when(authService.getUserProfile(anyString()))
//                .thenReturn(Optional.empty());
//
//        mockMvc.perform(get("/auth/profile")
//                        .header("X-User-Id", "999")
//                        .header("X-User-Email", "nonexistent@example.com"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.error").value("User profile not found."));
//    }
//
//    // ===== HEALTH CHECK TESTS =====
//
//    @Test
//    void healthCheck_ReturnsHealthStatus() throws Exception {
//        mockMvc.perform(get("/auth/health"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("UP"))
//                .andExpect(jsonPath("$.service").value("auth-service"));
//    }
//}