package com.ibra.resturantservice.service;

import com.ibra.exception.BusinessException;
import com.ibra.exception.ResourceNotFoundException;
import com.ibra.exception.UnauthorizedException;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.dto.RestaurantDTO;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.enums.RestaurantStatus;
import com.ibra.resturantservice.mapper.RestaurantMapper;
import com.ibra.resturantservice.respository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantMapper restaurantMapper;

    @InjectMocks
    private RestaurantService restaurantService;

    private CreateRestaurantRequest validRequest;
    private Restaurant savedRestaurant;
    private RestaurantDTO restaurantDTO;

    @BeforeEach
    void setUp() {
        // Valid request (matches DTO validation rules)
        validRequest = new CreateRestaurantRequest(
                "Test Restaurant",
                "A test description",
                "123 Main St",
                "+1234567890",
                "test@example.com",
                "http://image.url"
        );

        // Saved entity (simulates DB state)
        savedRestaurant = new Restaurant(
                validRequest.getName(),
                validRequest.getDescription(),
                validRequest.getAddress(),
                validRequest.getPhoneNumber(),
                validRequest.getEmail(),
                1L // ownerId
        );
        savedRestaurant.setId(1L);
        savedRestaurant.setStatus(RestaurantStatus.PENDING_APPROVAL);
        savedRestaurant.setCreatedAt(LocalDateTime.now());
        savedRestaurant.setUpdatedAt(LocalDateTime.now());

        // Response DTO
        restaurantDTO = new RestaurantDTO(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getDescription(),
                savedRestaurant.getAddress(),
                savedRestaurant.getPhoneNumber(),
                savedRestaurant.getEmail(),
                savedRestaurant.getOwnerId(),
                null, // ownerName
                null, // ownerEmail
                savedRestaurant.getImageUrl(),
                savedRestaurant.getStatus(),
                savedRestaurant.getCreatedAt(),
                savedRestaurant.getUpdatedAt(),
                Collections.emptyList(), // menuItems
                null, // cuisine
                null  // rate
        );
    }

    // ------------------------- CREATE TESTS -------------------------
    @Test
    void createRestaurant_ValidRequest_ReturnsRestaurantDTO() {
        // Arrange
        when(restaurantRepository.existsByPhoneNumber(validRequest.getPhoneNumber())).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);
        when(restaurantMapper.toDTO(savedRestaurant)).thenReturn(restaurantDTO);

        // Act
        RestaurantDTO result = restaurantService.createRestaurant(validRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Restaurant", result.getName());
        assertEquals(RestaurantStatus.PENDING_APPROVAL, result.getStatus());
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_DuplicatePhone_ThrowsBusinessException() {
        // Arrange
        when(restaurantRepository.existsByPhoneNumber(validRequest.getPhoneNumber())).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                restaurantService.createRestaurant(validRequest, 1L)
        );
    }

    // ------------------------- READ TESTS -------------------------
    @Test
    void getRestaurantById_ValidId_ReturnsRestaurantDTO() {
        // Arrange
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(savedRestaurant));
        when(restaurantMapper.toDTO(savedRestaurant)).thenReturn(restaurantDTO);

        // Act
        RestaurantDTO result = restaurantService.getRestaurantById(1L);

        // Assert
        assertEquals(restaurantDTO, result);
        verify(restaurantRepository, times(1)).findById(1L);
    }

    @Test
    void getRestaurantById_InvalidId_ThrowsResourceNotFoundException() {
        // Arrange
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                restaurantService.getRestaurantById(999L)
        );
    }

    @Test
    void getAllActiveRestaurants_ReturnsActiveRestaurantsOnly() {
        // Arrange
        Restaurant activeRestaurant = new Restaurant();
        activeRestaurant.setStatus(RestaurantStatus.ACTIVE);
        List<Restaurant> activeRestaurants = List.of(activeRestaurant);

        when(restaurantRepository.findByStatus(RestaurantStatus.ACTIVE)).thenReturn(activeRestaurants);
        when(restaurantMapper.toDTO(any(Restaurant.class))).thenReturn(new RestaurantDTO());

        // Act
        List<RestaurantDTO> result = restaurantService.getAllActiveRestaurants();

        // Assert
        assertEquals(1, result.size());
        verify(restaurantRepository, times(1)).findByStatus(RestaurantStatus.ACTIVE);
    }

    // ------------------------- UPDATE TESTS -------------------------
    @Test
    void updateRestaurant_ValidRequest_UpdatesFields() {
        // Arrange
        CreateRestaurantRequest updateRequest = new CreateRestaurantRequest(
                "Updated Name",
                "Updated Description",
                "456 New St",
                "+9876543210",
                "updated@example.com",
                "http://new-image.url"
        );

        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(savedRestaurant));
        when(restaurantRepository.existsByPhoneNumber(updateRequest.getPhoneNumber())).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);
        when(restaurantMapper.toDTO(any(Restaurant.class))).thenReturn(restaurantDTO);

        // Act
        RestaurantDTO result = restaurantService.updateRestaurant(1L, updateRequest, 1L);

        // Assert
        assertEquals(restaurantDTO, result);
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void updateRestaurant_UnauthorizedOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(restaurantRepository.findByIdAndOwnerId(1L, 2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                restaurantService.updateRestaurant(1L, validRequest, 2L)
        );
    }

    // ------------------------- DELETE TESTS -------------------------
    @Test
    void deleteRestaurant_ValidOwner_DeletesRestaurant() {
        // Arrange
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(savedRestaurant));

        // Act
        restaurantService.deleteRestaurant(1L, 1L);

        // Assert
        verify(restaurantRepository, times(1)).delete(savedRestaurant);
    }

    @Test
    void deleteRestaurant_InvalidOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(restaurantRepository.findByIdAndOwnerId(1L, 2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                restaurantService.deleteRestaurant(1L, 2L)
        );
    }

    // ------------------------- STATUS UPDATE TESTS -------------------------
    @Test
    void updateRestaurantStatus_AdminUpdates_SavesNewStatus() {
        // Arrange
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(savedRestaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);
        when(restaurantMapper.toDTO(any(Restaurant.class))).thenReturn(restaurantDTO);

        // Act
        RestaurantDTO result = restaurantService.updateRestaurantStatus(1L, RestaurantStatus.ACTIVE);

        // Assert
        assertEquals(restaurantDTO, result);
        verify(restaurantRepository, times(1)).save(savedRestaurant);
    }
}