package com.ibra.resturantservice.service;

import static org.junit.jupiter.api.Assertions.*;
import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.dto.MenuItemDTO;
import com.ibra.resturantservice.entity.MenuItem;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.enums.MenuItemCategory;
import com.ibra.enums.MenuItemStatus;
import com.ibra.resturantservice.exception.ResourceNotFoundException;
import com.ibra.resturantservice.exception.UnauthorizedException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

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

    private CreateMenuItemRequest createRequest;
    private MenuItem menuItem;
    private MenuItemDTO menuItemDTO;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        createRequest = new CreateMenuItemRequest();
        createRequest.setName("Test Item");
        createRequest.setDescription("Test Description");
        createRequest.setPrice(new BigDecimal("15.99"));
        createRequest.setCategory(MenuItemCategory.MAIN_COURSE);
        createRequest.setImageUrl("http://test.com/image.jpg");

        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Test Restaurant");
        restaurant.setOwnerId(1L);

        menuItem = new MenuItem();
        menuItem.setId(1L);
        menuItem.setName("Test Item");
        menuItem.setPrice(new BigDecimal("15.99"));
        menuItem.setCategory(MenuItemCategory.MAIN_COURSE);
        menuItem.setStatus(MenuItemStatus.AVAILABLE);
        menuItem.setRestaurant(restaurant);

        menuItemDTO = new MenuItemDTO();
        menuItemDTO.setId(1L);
        menuItemDTO.setName("Test Item");
        menuItemDTO.setPrice(new BigDecimal("15.99"));
        menuItemDTO.setCategory(MenuItemCategory.MAIN_COURSE);
        menuItemDTO.setStatus(MenuItemStatus.AVAILABLE);
    }

    @Test
    void createMenuItem_Success() {
        // Given
        doNothing().when(restaurantService).validateRestaurantOwnership(1L, 1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);
        when(menuItemMapper.toDTO(any(MenuItem.class))).thenReturn(menuItemDTO);

        // When
        MenuItemDTO result = menuItemService.createMenuItem(1L, createRequest, 1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        assertEquals(new BigDecimal("15.99"), result.getPrice());
        assertEquals(MenuItemCategory.MAIN_COURSE, result.getCategory());
        verify(restaurantService).validateRestaurantOwnership(1L, 1L);
        verify(restaurantRepository).findById(1L);
        verify(menuItemRepository).save(any(MenuItem.class));
    }

    @Test
    void createMenuItem_ThrowsResourceNotFoundException_WhenRestaurantNotFound() {
        // Given
        doNothing().when(restaurantService).validateRestaurantOwnership(1L, 1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> menuItemService.createMenuItem(1L, createRequest, 1L));

        assertEquals("Restaurant not found with ID: 1", exception.getMessage());
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void createMenuItem_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        doThrow(new UnauthorizedException("You don't own this restaurant"))
                .when(restaurantService).validateRestaurantOwnership(1L, 1L);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> menuItemService.createMenuItem(1L, createRequest, 1L));

        assertEquals("You don't own this restaurant", exception.getMessage());
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void getMenuItemById_Success() {
        // Given
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(menuItemMapper.toDTO(menuItem)).thenReturn(menuItemDTO);

        // When
        MenuItemDTO result = menuItemService.getMenuItemById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        verify(menuItemRepository).findById(1L);
    }

    @Test
    void getMenuItemById_ThrowsResourceNotFoundException() {
        // Given
        when(menuItemRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> menuItemService.getMenuItemById(1L));

        assertEquals("Menu item not found with ID: 1", exception.getMessage());
    }

    @Test
    void getMenuItemsByRestaurant_Success() {
        // Given
        List<MenuItem> menuItems = Arrays.asList(menuItem);
        List<MenuItemDTO> menuItemDTOs = Arrays.asList(menuItemDTO);
        when(menuItemRepository.findByRestaurantIdAndStatus(1L, MenuItemStatus.AVAILABLE))
                .thenReturn(menuItems);
        when(menuItemMapper.toDTO(menuItem)).thenReturn(menuItemDTO);

        // When
        List<MenuItemDTO> result = menuItemService.getMenuItemsByRestaurant(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getName());
        verify(menuItemRepository).findByRestaurantIdAndStatus(1L, MenuItemStatus.AVAILABLE);
    }

    @Test
    void getMenuItemsByCategory_Success() {
        // Given
        List<MenuItem> menuItems = Arrays.asList(menuItem);
        when(menuItemRepository.findAvailableItemsByRestaurantAndCategory(1L, MenuItemCategory.MAIN_COURSE))
                .thenReturn(menuItems);
        when(menuItemMapper.toDTO(menuItem)).thenReturn(menuItemDTO);

        // When
        List<MenuItemDTO> result = menuItemService.getMenuItemsByCategory(1L, MenuItemCategory.MAIN_COURSE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(MenuItemCategory.MAIN_COURSE, result.get(0).getCategory());
        verify(menuItemRepository).findAvailableItemsByRestaurantAndCategory(1L, MenuItemCategory.MAIN_COURSE);
    }

    @Test
    void updateMenuItem_Success() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);
        when(menuItemMapper.toDTO(any(MenuItem.class))).thenReturn(menuItemDTO);

        // When
        MenuItemDTO result = menuItemService.updateMenuItem(1L, createRequest, 1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        verify(menuItemRepository).findByIdAndRestaurantOwnerId(1L, 1L);
        verify(menuItemRepository).save(menuItem);
    }

    @Test
    void updateMenuItem_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> menuItemService.updateMenuItem(1L, createRequest, 1L));

        assertEquals("You can only update menu items for your own restaurants", exception.getMessage());
    }

    @Test
    void updateMenuItemStatus_Success() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);
        when(menuItemMapper.toDTO(any(MenuItem.class))).thenReturn(menuItemDTO);

        // When
        MenuItemDTO result = menuItemService.updateMenuItemStatus(1L, MenuItemStatus.OUT_OF_STOCK, 1L);

        // Then
        assertNotNull(result);
        verify(menuItemRepository).findByIdAndRestaurantOwnerId(1L, 1L);
        verify(menuItemRepository).save(menuItem);
        assertEquals(MenuItemStatus.OUT_OF_STOCK, menuItem.getStatus());
    }

    @Test
    void updateMenuItemStatus_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> menuItemService.updateMenuItemStatus(1L, MenuItemStatus.OUT_OF_STOCK, 1L));

        assertEquals("You can only update menu items for your own restaurants", exception.getMessage());
    }

    @Test
    void deleteMenuItem_Success() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.of(menuItem));

        // When
        menuItemService.deleteMenuItem(1L, 1L);

        // Then
        verify(menuItemRepository).findByIdAndRestaurantOwnerId(1L, 1L);
        verify(menuItemRepository).delete(menuItem);
    }

    @Test
    void deleteMenuItem_ThrowsUnauthorizedException_WhenNotOwner() {
        // Given
        when(menuItemRepository.findByIdAndRestaurantOwnerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> menuItemService.deleteMenuItem(1L, 1L));

        assertEquals("You can only delete menu items for your own restaurants", exception.getMessage());
        verify(menuItemRepository, never()).delete(any(MenuItem.class));
    }

    @Test
    void searchMenuItemsByName_Success() {
        // Given
        List<MenuItem> menuItems = Arrays.asList(menuItem);
        when(menuItemRepository.searchByNameInRestaurant(1L, "Test")).thenReturn(menuItems);
        when(menuItemMapper.toDTO(menuItem)).thenReturn(menuItemDTO);

        // When
        List<MenuItemDTO> result = menuItemService.searchMenuItemsByName(1L, "Test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getName());
        verify(menuItemRepository).searchByNameInRestaurant(1L, "Test");
    }

    @Test
    void fallbackGetMenuItems_ReturnsEmptyList() {
        // When
        List<MenuItemDTO> result = menuItemService.fallbackGetMenuItems(1L, new RuntimeException("Test exception"));

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}