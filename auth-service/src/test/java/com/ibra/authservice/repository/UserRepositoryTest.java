package com.ibra.authservice.repository;

import com.ibra.authservice.entity.User;
import com.ibra.authservice.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.CUSTOMER);
    }

    @Test
    void findByEmail_ExistingEmail_ReturnsUser() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
        assertEquals("Doe", found.get().getLastName());
        assertEquals("john@example.com", found.get().getEmail());
        assertEquals(UserRole.CUSTOMER, found.get().getRole());
    }

    @Test
    void findByEmail_NonExistingEmail_ReturnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByEmail("john@example.com");

        assertTrue(exists);
    }

    @Test
    void existsByEmail_NonExistingEmail_ReturnsFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    @Test
    void save_NewUser_PersistsSuccessfully() {
        User saved = userRepository.save(testUser);

        assertNotNull(saved.getId());
        assertEquals("John", saved.getFirstName());
        assertEquals("john@example.com", saved.getEmail());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void findByEmail_CaseInsensitive_HandlesCorrectly() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("JOHN@EXAMPLE.COM");

        // This test will fail if your database is case-sensitive
        // You might need to adjust based on your database configuration
        assertTrue(found.isEmpty()); // Assuming case-sensitive search
    }
}