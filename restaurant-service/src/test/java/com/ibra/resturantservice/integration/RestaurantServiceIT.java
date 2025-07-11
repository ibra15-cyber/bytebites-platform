package com.ibra.resturantservice.integration;

import com.ibra.exception.BusinessException;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.dto.RestaurantDTO;
import com.ibra.enums.RestaurantStatus;
import com.ibra.resturantservice.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class RestaurantServiceIT extends AbstractIntegrationTest {

    @Autowired
    private RestaurantService restaurantService;

    @Test
    void createRestaurant_ValidRequest_SavesToDatabase() {
        // Arrange
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Test Restaurant",
                "Description",
                "123 Main St",
                "+1234567890",
                "test@example.com",
                "http://image.url"
        );

        // Act
        RestaurantDTO savedRestaurant = restaurantService.createRestaurant(request, 1L);

        // Assert
        assertNotNull(savedRestaurant.getId());
        assertEquals(RestaurantStatus.PENDING_APPROVAL, savedRestaurant.getStatus());
    }

    @Test
    void createRestaurant_DuplicatePhone_ThrowsException() {
        // Arrange
        CreateRestaurantRequest request1 = new CreateRestaurantRequest(
                "Restaurant 1", "Desc", "Address 1", "+1234567890", "email1@test.com", "http://image1.url"
        );
        restaurantService.createRestaurant(request1, 1L);

        CreateRestaurantRequest request2 = new CreateRestaurantRequest(
                "Restaurant 2", "Desc", "Address 2", "+1234567890", "email2@test.com", "http://image2.url"
        );

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                restaurantService.createRestaurant(request2, 2L)
        );
    }

    @Test
    void updateRestaurantStatus_AdminApproval_UpdatesStatus() {
        // Arrange
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Test Restaurant", "Desc", "Address", "+1234567890", "test@example.com", "http://image.url"
        );
        RestaurantDTO restaurant = restaurantService.createRestaurant(request, 1L);

        // Act
        RestaurantDTO updated = restaurantService.updateRestaurantStatus(
                restaurant.getId(),
                RestaurantStatus.ACTIVE
        );

        // Assert
        assertEquals(RestaurantStatus.ACTIVE, updated.getStatus());
    }
}