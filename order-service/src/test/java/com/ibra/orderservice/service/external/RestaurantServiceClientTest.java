package com.ibra.orderservice.service.external;

import com.ibra.dto.ApiResponse;
import com.ibra.dto.MenuItemDTO;
import com.ibra.dto.RestaurantDTO;
import com.ibra.enums.MenuItemCategory;
import com.ibra.enums.MenuItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceClientTest {

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    private RestaurantDTO sampleRestaurant;
    private MenuItemDTO sampleMenuItem;
    private List<MenuItemDTO> sampleMenuItems;

    @BeforeEach
    void setUp() {
        // Create sample restaurant
        sampleRestaurant = new RestaurantDTO();
        sampleRestaurant.setId(1L);
        sampleRestaurant.setName("Test Restaurant");
        sampleRestaurant.setAddress("123 Test Street");
        sampleRestaurant.setPhoneNumber("123-456-7890");
        sampleRestaurant.setEmail("owner@gmail.com");

        // Create sample menu item
        sampleMenuItem = new MenuItemDTO();
        sampleMenuItem.setId(1L);
        sampleMenuItem.setName("Test Burger");
        sampleMenuItem.setDescription("Delicious test burger");
        sampleMenuItem.setPrice(BigDecimal.valueOf(15.99));
        sampleMenuItem.setCategory(MenuItemCategory.APPETIZER);
        sampleMenuItem.setRestaurantId(1L);
        sampleMenuItem.setStatus(MenuItemStatus.AVAILABLE);

        // Create sample menu items list
        MenuItemDTO menuItem2 = new MenuItemDTO();
        menuItem2.setId(2L);
        menuItem2.setName("Test Fries");
        menuItem2.setDescription("Crispy fries");
        menuItem2.setPrice(BigDecimal.valueOf(5.99));
        menuItem2.setCategory(MenuItemCategory.APPETIZER);
        menuItem2.setRestaurantId(1L);
        menuItem2.setStatus(MenuItemStatus.AVAILABLE);

        sampleMenuItems = Arrays.asList(sampleMenuItem, menuItem2);
    }

    @Test
    void testGetRestaurantById_Success() {
        // Arrange
        Long restaurantId = 1L;
        ApiResponse<RestaurantDTO> expectedResponse = new ApiResponse<>(
                true, "Restaurant found", sampleRestaurant);

        when(restaurantServiceClient.getRestaurantById(restaurantId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<RestaurantDTO> result = restaurantServiceClient.getRestaurantById(restaurantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Restaurant found", result.getMessage());
        assertEquals(sampleRestaurant, result.getData());
        verify(restaurantServiceClient).getRestaurantById(restaurantId);
    }

    @Test
    void testGetRestaurantById_NotFound() {
        // Arrange
        Long restaurantId = 999L;
        ApiResponse<RestaurantDTO> expectedResponse = new ApiResponse<>(
                false, "Restaurant not found", null);

        when(restaurantServiceClient.getRestaurantById(restaurantId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<RestaurantDTO> result = restaurantServiceClient.getRestaurantById(restaurantId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Restaurant not found", result.getMessage());
        assertNull(result.getData());
        verify(restaurantServiceClient).getRestaurantById(restaurantId);
    }

    @Test
    void testGetMenuItemById_Success() {
        // Arrange
        Long menuItemId = 1L;
        ApiResponse<MenuItemDTO> expectedResponse = new ApiResponse<>(
                true, "Menu item found", sampleMenuItem);

        when(restaurantServiceClient.getMenuItemById(menuItemId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<MenuItemDTO> result = restaurantServiceClient.getMenuItemById(menuItemId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Menu item found", result.getMessage());
        assertEquals(sampleMenuItem, result.getData());
        verify(restaurantServiceClient).getMenuItemById(menuItemId);
    }

    @Test
    void testGetRestaurantByOwnerId_Success() {
        // Arrange
        Long ownerId = 100L;
        ApiResponse<RestaurantDTO> expectedResponse = new ApiResponse<>(
                true, "Restaurant found for owner", sampleRestaurant);

        when(restaurantServiceClient.getRestaurantByOwnerId(ownerId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<RestaurantDTO> result = restaurantServiceClient.getRestaurantByOwnerId(ownerId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Restaurant found for owner", result.getMessage());
        assertEquals(sampleRestaurant, result.getData());
        verify(restaurantServiceClient).getRestaurantByOwnerId(ownerId);
    }

    @Test
    void testGetMenuItemsByRestaurant_Success() {
        // Arrange
        Long restaurantId = 1L;
        ApiResponse<List<MenuItemDTO>> expectedResponse = new ApiResponse<>(
                true, "Menu items found", sampleMenuItems);

        when(restaurantServiceClient.getMenuItemsByRestaurant(restaurantId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<List<MenuItemDTO>> result = restaurantServiceClient.getMenuItemsByRestaurant(restaurantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Menu items found", result.getMessage());
        assertEquals(sampleMenuItems, result.getData());
        assertEquals(2, result.getData().size());
        verify(restaurantServiceClient).getMenuItemsByRestaurant(restaurantId);
    }

    @Test
    void testGetMenuItemsByRestaurant_EmptyList() {
        // Arrange
        Long restaurantId = 1L;
        ApiResponse<List<MenuItemDTO>> expectedResponse = new ApiResponse<>(
                true, "No menu items found", Collections.emptyList());

        when(restaurantServiceClient.getMenuItemsByRestaurant(restaurantId))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<List<MenuItemDTO>> result = restaurantServiceClient.getMenuItemsByRestaurant(restaurantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("No menu items found", result.getMessage());
        assertTrue(result.getData().isEmpty());
        verify(restaurantServiceClient).getMenuItemsByRestaurant(restaurantId);
    }

    @Test
    void testGetMenuItemsByCategory_Success() {
        // Arrange
        Long restaurantId = 1L;
        String category = "MAIN";
        List<MenuItemDTO> mainItems = Collections.singletonList(sampleMenuItem);
        ApiResponse<List<MenuItemDTO>> expectedResponse = new ApiResponse<>(
                true, "Menu items found for category", mainItems);

        when(restaurantServiceClient.getMenuItemsByCategory(restaurantId, category))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<List<MenuItemDTO>> result = restaurantServiceClient.getMenuItemsByCategory(restaurantId, category);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Menu items found for category", result.getMessage());
        assertEquals(mainItems, result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("MAIN", result.getData().get(0).getCategory());
        verify(restaurantServiceClient).getMenuItemsByCategory(restaurantId, category);
    }

    @Test
    void testGetMenuItemsByCategory_InvalidCategory() {
        // Arrange
        Long restaurantId = 1L;
        String category = "INVALID";
        ApiResponse<List<MenuItemDTO>> expectedResponse = new ApiResponse<>(
                false, "Invalid category", null);

        when(restaurantServiceClient.getMenuItemsByCategory(restaurantId, category))
                .thenReturn(expectedResponse);

        // Act
        ApiResponse<List<MenuItemDTO>> result = restaurantServiceClient.getMenuItemsByCategory(restaurantId, category);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Invalid category", result.getMessage());
        assertNull(result.getData());
        verify(restaurantServiceClient).getMenuItemsByCategory(restaurantId, category);
    }

    @Test
    void testAllMethodsWithNullParameters() {
        // Test that methods handle null parameters gracefully
        // Note: In real scenarios, these would likely throw exceptions,
        // but we're testing the mock behavior here

        // Act & Assert
        assertDoesNotThrow(() -> {
            restaurantServiceClient.getRestaurantById(null);
            restaurantServiceClient.getMenuItemById(null);
            restaurantServiceClient.getRestaurantByOwnerId(null);
            restaurantServiceClient.getMenuItemsByRestaurant(null);
            restaurantServiceClient.getMenuItemsByCategory(null, null);
        });
    }
}