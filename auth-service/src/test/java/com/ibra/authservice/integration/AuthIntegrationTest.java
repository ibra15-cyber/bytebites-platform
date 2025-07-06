package com.ibra.authservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibra.authservice.dto.LoginRequest;
import com.ibra.authservice.dto.RegisterRequest;
import com.ibra.authservice.entity.User;
import com.ibra.authservice.enums.UserRole;
import com.ibra.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void completeUserJourney_RegisterLoginProfile_Success() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.CUSTOMER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Please log in."));

        // 2. Login with the registered user
        LoginRequest loginRequest = new LoginRequest("jane@example.com", "password123");

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();

        // 3. Get user profile (simulating gateway headers)
        mockMvc.perform(get("/auth/profile")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerWithExistingEmail_ReturnsBadRequest() throws Exception {
        // Create a user in the database
        User existingUser = new User();
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setRole(UserRole.CUSTOMER);
        userRepository.save(existingUser);

        // Try to register with the same email
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setEmail("existing@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.CUSTOMER);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already taken!"));
    }



    @Test
    void registerWithDifferentRoles_Success() throws Exception {
        // Test registering as ADMIN
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setFirstName("Admin");
        adminRequest.setLastName("User");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password123");
        adminRequest.setRole(UserRole.ADMIN);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Please log in."));

        // Login as admin
        LoginRequest loginRequest = new LoginRequest("admin@example.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void getProfileWithCorrectHeaders_Success() throws Exception {
        // Create a user
        User user = new User();
        user.setFirstName("Profile");
        user.setLastName("Test");
        user.setEmail("profile@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);

        // Get profile with correct headers
        mockMvc.perform(get("/auth/profile")
                        .header("X-User-Id", savedUser.getId().toString())
                        .header("X-User-Email", "profile@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.firstName").value("Profile"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}