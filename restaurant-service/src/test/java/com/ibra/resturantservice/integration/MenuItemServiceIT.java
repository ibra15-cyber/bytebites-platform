package com.ibra.resturantservice.integration;

import com.ibra.dto.RestaurantDTO;
import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.dto.MenuItemDTO;
import com.ibra.enums.MenuItemCategory;
import com.ibra.enums.MenuItemStatus;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.resturantservice.service.MenuItemService;
import com.ibra.resturantservice.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class MenuItemServiceIT extends AbstractIntegrationTest {

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private RestaurantService restaurantService;

    @Test
    void createMenuItem_ValidRequest_SavesToDatabase() {
        // Arrange
        Long restaurantId = createTestRestaurant();
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger", "Delicious", BigDecimal.valueOf(9.99),
                MenuItemCategory.MAIN_COURSE, "http://image.url"
        );

        // Act
        MenuItemDTO savedItem = menuItemService.createMenuItem(restaurantId, request, 1L);

        // Assert
        assertNotNull(savedItem.getId());
        assertEquals(MenuItemStatus.AVAILABLE, savedItem.getStatus());
    }

    @Test
    void updateMenuItemStatus_OwnerRequest_UpdatesStatus() {
        // Arrange
        Long restaurantId = createTestRestaurant();
        MenuItemDTO item = createTestMenuItem(restaurantId);

        // Act
        MenuItemDTO updated = menuItemService.updateMenuItemStatus(
                item.getId(),
                MenuItemStatus.OUT_OF_STOCK,
                1L
        );

        // Assert
        assertEquals(MenuItemStatus.OUT_OF_STOCK, updated.getStatus());
    }

    private Long createTestRestaurant() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Test Restaurant", "Desc", "Address", "+1234567890", "test@example.com", "http://image.url"
        );
        RestaurantDTO restaurant = restaurantService.createRestaurant(request, 1L);
        return restaurant.getId();
    }

    private MenuItemDTO createTestMenuItem(Long restaurantId) {
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Pizza", "Cheesy", BigDecimal.valueOf(12.99),
                MenuItemCategory.MAIN_COURSE, "http://image.url"
        );
        return menuItemService.createMenuItem(restaurantId, request, 1L);
    }
}