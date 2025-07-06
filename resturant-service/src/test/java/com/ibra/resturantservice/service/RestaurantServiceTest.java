package com.ibra.resturantservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.resturantservice.dto.RestaurantDTO;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.resturantservice.enums.RestaurantStatus;
import com.ibra.resturantservice.exception.BusinessException;
import com.ibra.resturantservice.exception.ResourceNotFoundException;
import com.ibra.resturantservice.exception.UnauthorizedException;
import com.ibra.resturantservice.mapper.RestaurantMapper;
import com.ibra.resturantservice.respository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantMapper restaurantMapper;

    @InjectMocks
    private RestaurantService restaurantService;

    private CreateRestaurantRequest createRequest;
    private Restaurant restaurant;
    private RestaurantDTO restaurantDTO;

    @BeforeEach
    void setUp() {
        createRequest = new CreateRestaurantRequest();
        createRequest.setName("Test Restaurant");
        createRequest.setDescription("Test Description");
        createRequest.setAddress("Test Address");
        createRequest.setPhoneNumber("1234567890");
        createRequest.setEmail("test@restaurant.com");
        createRequest.setImageUrl("http://test.com/image.jpg");

        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Test Restaurant");
        restaurant.setEmail("test@restaurant.com");
        restaurant.setPhoneNumber("1234567890");
        restaurant.setOwnerId(1L);
        restaurant.setStatus(RestaurantStatus.PENDING_APPROVAL);

        restaurantDTO = new RestaurantDTO();
        restaurantDTO.setId(1L);
        restaurantDTO.setName("Test Restaurant");
    }

    @Test
    void createRestaurant_Success() {
        // Given
        when(restaurantRepository.existsByEmail(anyString())).thenReturn(false);
        when(restaurantRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(restaurantMapper.toDTO(any(Restaurant.class))).thenReturn(restaurantDTO);

        // When
        RestaurantDTO result = restaurantService.createRestaurant(createRequest, 1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Restaurant", result.getName());
        verify(restaurantRepository).existsByEmail("test@restaurant.com");
        verify(restaurantRepository).existsByPhoneNumber("1234567890");
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_ThrowsBusinessException_WhenEmailExists() {
        // Given
        when(restaurantRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> restaurantService.createRestaurant(createRequest, 1L));

        assertEquals("Restaurant with this email already exists", exception.getMessage());
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_ThrowsBusinessException_WhenPhoneExists() {
        // Given
        when(restaurantRepository.existsByEmail(anyString())).thenReturn(false);
        when(restaurantRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> restaurantService.createRestaurant(createRequest, 1L));

        assertEquals("Restaurant with this phone number already exists", exception.getMessage());
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    void getRestaurantById_Success() {
        // Given
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantMapper.toDTO(restaurant)).thenReturn(restaurantDTO);

        // When
        RestaurantDTO result = restaurantService.getRestaurantById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Restaurant", result.getName());
        verify(restaurantRepository).findById(1L);
    }

    @Test
    void getRestaurantById_ThrowsResourceNotFoundException() {
        // Given
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> restaurantService.getRestaurantById(1L));

        assertEquals("Restaurant not found with ID: 1", exception.getMessage());
    }


    @Test
    void updateRestaurant_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> restaurantService.updateRestaurant(1L, createRequest, 1L));

        assertEquals("You can only update your own restaurants", exception.getMessage());
    }

    @Test
    void validateRestaurantOwnership_Success() {
        // Given
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(restaurant));

        // When & Then
        assertDoesNotThrow(() -> restaurantService.validateRestaurantOwnership(1L, 1L));
    }

    @Test
    void validateRestaurantOwnership_ThrowsUnauthorizedException() {
        // Given
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> restaurantService.validateRestaurantOwnership(1L, 1L));

        assertEquals("You don't own this restaurant", exception.getMessage());
    }

    @Test
    void updateRestaurantStatus_Success() {
        // Given
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(restaurantMapper.toDTO(any(Restaurant.class))).thenReturn(restaurantDTO);

        // When
        RestaurantDTO result = restaurantService.updateRestaurantStatus(1L, RestaurantStatus.ACTIVE);

        // Then
        assertNotNull(result);
        verify(restaurantRepository).findById(1L);
        verify(restaurantRepository).save(restaurant);
        assertEquals(RestaurantStatus.ACTIVE, restaurant.getStatus());
    }

    @Test
    void deleteRestaurant_Success() {
        // Given
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(restaurant));

        // When
        restaurantService.deleteRestaurant(1L, 1L);

        // Then
        verify(restaurantRepository).findByIdAndOwnerId(1L, 1L);
        verify(restaurantRepository).delete(restaurant);
    }

    @Test
    void deleteRestaurant_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        when(restaurantRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> restaurantService.deleteRestaurant(1L, 1L));

        assertEquals("You can only delete your own restaurants", exception.getMessage());
        verify(restaurantRepository, never()).delete(any(Restaurant.class));
    }
}