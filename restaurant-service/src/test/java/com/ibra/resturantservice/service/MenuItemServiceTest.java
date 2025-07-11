package com.ibra.resturantservice.service;

import com.ibra.exception.ResourceNotFoundException;
import com.ibra.exception.UnauthorizedException;
import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.dto.MenuItemDTO;
import com.ibra.resturantservice.entity.MenuItem;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.enums.MenuItemCategory;
import com.ibra.enums.MenuItemStatus;
import com.ibra.resturantservice.mapper.MenuItemMapper;
import com.ibra.resturantservice.respository.MenuItemRepository;
import com.ibra.resturantservice.respository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemMapper menuItemMapper;

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private MenuItemService menuItemService;

    private CreateMenuItemRequest validRequest;
    private MenuItem savedMenuItem;
    private MenuItemDTO menuItemDTO;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        // Valid request (matches DTO validation rules)
        validRequest = new CreateMenuItemRequest(
                "Test Burger",
                "Delicious beef burger",
                BigDecimal.valueOf(9.99),
                MenuItemCategory.MAIN_COURSE,
                "http://image.url"
        );

        // Restaurant (required for ownership validation)
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setOwnerId(1L);

        // Saved entity (simulates DB state)
        savedMenuItem = new MenuItem(
                validRequest.getName(),
                validRequest.getDescription(),
                validRequest.getPrice(),
                validRequest.getCategory(),
                restaurant
        );
        savedMenuItem.setId(1L);
        savedMenuItem.setStatus(MenuItemStatus.AVAILABLE);
        savedMenuItem.setImageUrl(validRequest.getImageUrl());
        savedMenuItem.setCreatedAt(LocalDateTime.now());
        savedMenuItem.setUpdatedAt(LocalDateTime.now());

        // Response DTO
        menuItemDTO = new MenuItemDTO(
                savedMenuItem.getId(),
                savedMenuItem.getName(),
                savedMenuItem.getDescription(),
                savedMenuItem.getPrice(),
                restaurant.getId(),
                savedMenuItem.getCategory(),
                savedMenuItem.getImageUrl(),
                savedMenuItem.getStatus(),
                savedMenuItem.getCreatedAt(),
                savedMenuItem.getUpdatedAt()
        );
    }

    // ------------------------- CREATE TESTS -------------------------
    @Test
    void createMenuItem_ValidRequest_ReturnsMenuItemDTO() {
        // Arrange
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        MenuItemDTO result = menuItemService.createMenuItem(1L, validRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Burger", result.getName());
        assertEquals(MenuItemStatus.AVAILABLE, result.getStatus());
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void createMenuItem_InvalidRestaurant_ThrowsResourceNotFoundException() {
        // Arrange
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                menuItemService.createMenuItem(999L, validRequest, 1L)
        );
    }

    // ------------------------- READ TESTS -------------------------
    @Test
    void getMenuItemById_ValidId_ReturnsMenuItemDTO() {
        // Arrange
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(savedMenuItem));
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        MenuItemDTO result = menuItemService.getMenuItemById(1L);

        // Assert
        assertEquals(menuItemDTO, result);
        verify(menuItemRepository, times(1)).findById(1L);
    }

    @Test
    void getMenuItemsByRestaurant_ReturnsAvailableItemsOnly() {
        // Arrange
        MenuItem unavailableItem = new MenuItem();
        unavailableItem.setStatus(MenuItemStatus.OUT_OF_STOCK);

        when(menuItemRepository.findByRestaurantIdAndStatus(1L, MenuItemStatus.AVAILABLE))
                .thenReturn(List.of(savedMenuItem));
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        List<MenuItemDTO> result = menuItemService.getMenuItemsByRestaurant(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Burger", result.get(0).getName());
    }

    // ------------------------- UPDATE TESTS -------------------------
    @Test
    void updateMenuItem_ValidRequest_UpdatesFields() {
        // Arrange
        CreateMenuItemRequest updateRequest = new CreateMenuItemRequest(
                "Updated Burger",
                "Now with cheese",
                BigDecimal.valueOf(10.99),
                MenuItemCategory.MAIN_COURSE,
                "http://new-image.url"
        );

        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L))
                .thenReturn(Optional.of(savedMenuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        MenuItemDTO result = menuItemService.updateMenuItem(1L, updateRequest, 1L);

        // Assert
        assertEquals(menuItemDTO, result);
        verify(menuItemRepository, times(1)).save(savedMenuItem);
    }

    @Test
    void updateMenuItem_UnauthorizedOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                menuItemService.updateMenuItem(1L, validRequest, 2L)
        );
    }

    // ------------------------- STATUS UPDATE TESTS -------------------------
    @Test
    void updateMenuItemStatus_ValidTransition_UpdatesStatus() {
        // Arrange
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L))
                .thenReturn(Optional.of(savedMenuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(savedMenuItem);
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        MenuItemDTO result = menuItemService.updateMenuItemStatus(1L, MenuItemStatus.OUT_OF_STOCK, 1L);

        // Assert
        assertEquals(menuItemDTO, result);
        assertEquals(MenuItemStatus.OUT_OF_STOCK, savedMenuItem.getStatus());
    }

    // ------------------------- DELETE TESTS -------------------------
    @Test
    void deleteMenuItem_ValidOwner_DeletesMenuItem() {
        // Arrange
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L))
                .thenReturn(Optional.of(savedMenuItem));

        // Act
        menuItemService.deleteMenuItem(1L, 1L);

        // Assert
        verify(menuItemRepository, times(1)).delete(savedMenuItem);
    }

    @Test
    void deleteMenuItem_InvalidOwner_ThrowsUnauthorizedException() {
        // Arrange
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                menuItemService.deleteMenuItem(1L, 2L)
        );
    }

    // ------------------------- SEARCH TESTS -------------------------
    @Test
    void searchMenuItemsByName_ReturnsMatchingItems() {
        // Arrange
        when(menuItemRepository.searchByNameInRestaurant(1L, "Burger"))
                .thenReturn(List.of(savedMenuItem));
        when(menuItemMapper.toDTO(savedMenuItem)).thenReturn(menuItemDTO);

        // Act
        List<MenuItemDTO> result = menuItemService.searchMenuItemsByName(1L, "Burger");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Burger", result.get(0).getName());
    }
}