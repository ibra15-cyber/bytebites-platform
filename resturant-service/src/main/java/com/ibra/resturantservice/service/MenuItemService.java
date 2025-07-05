package com.ibra.resturantservice.service;

import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.resturantservice.dto.MenuItemDTO;
import com.ibra.resturantservice.entity.MenuItem;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.resturantservice.enums.MenuItemCategory;
import com.ibra.resturantservice.enums.MenuItemStatus;
import com.ibra.resturantservice.exception.ResourceNotFoundException;
import com.ibra.resturantservice.exception.UnauthorizedException;
import com.ibra.resturantservice.mapper.MenuItemMapper;
import com.ibra.resturantservice.respository.MenuItemRepository;
import com.ibra.resturantservice.respository.RestaurantRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuItemService {

    private static final Logger logger = LoggerFactory.getLogger(MenuItemService.class);

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemMapper menuItemMapper;

    @Autowired
    private RestaurantService restaurantService;

    // Create menu item
    public MenuItemDTO createMenuItem(Long restaurantId, CreateMenuItemRequest request, Long ownerId) {
        logger.info("Creating new menu item for restaurant: {} by owner: {}", restaurantId, ownerId);

        // Validate restaurant ownership
        restaurantService.validateRestaurantOwnership(restaurantId, ownerId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + restaurantId));

        MenuItem menuItem = new MenuItem(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                restaurant
        );



        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setStatus(MenuItemStatus.AVAILABLE);

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        logger.info("Menu item created successfully with ID: {}", savedMenuItem.getId());

        return menuItemMapper.toDTO(savedMenuItem);
    }

    // Get all menu items for a restaurant
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getMenuItemsByRestaurant(Long restaurantId) {
        System.out.println("request reached here");
        logger.info("Fetching menu items for restaurant: {}", restaurantId);
        List<MenuItem> menuItems = menuItemRepository.findByRestaurantIdAndStatus(restaurantId, MenuItemStatus.AVAILABLE);
        return menuItems.stream()
                .map(menuItemMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get menu items by category
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getMenuItemsByCategory(Long restaurantId, MenuItemCategory category) {
        logger.info("Fetching menu items for restaurant: {} and category: {}", restaurantId, category);
        List<MenuItem> menuItems = menuItemRepository.findAvailableItemsByRestaurantAndCategory(restaurantId, category);
        return menuItems.stream()
                .map(menuItemMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get menu item by ID
    @Transactional(readOnly = true)
    public MenuItemDTO getMenuItemById(Long id) {
        System.out.println("request reached here");
        logger.info("Fetching menu item with ID: {}", id);
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with ID: " + id));

        return menuItemMapper.toDTO(menuItem);
    }

    // Update menu item (only by restaurant owner)
    public MenuItemDTO updateMenuItem(Long id, CreateMenuItemRequest request, Long ownerId) {
        logger.info("Updating menu item with ID: {} by owner: {}", id, ownerId);

        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantOwnerId(id, ownerId)
                .orElseThrow(() -> new UnauthorizedException("You can only update menu items for your own restaurants"));

        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setCategory(request.getCategory());
        menuItem.setImageUrl(request.getImageUrl());

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        logger.info("Menu item updated successfully with ID: {}", savedMenuItem.getId());

        return menuItemMapper.toDTO(savedMenuItem);
    }

    // Update menu item status
    public MenuItemDTO updateMenuItemStatus(Long id, MenuItemStatus status, Long ownerId) {
        logger.info("Updating menu item status for ID: {} to: {} by owner: {}", id, status, ownerId);

        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantOwnerId(id, ownerId)
                .orElseThrow(() -> new UnauthorizedException("You can only update menu items for your own restaurants"));

        menuItem.setStatus(status);
        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        logger.info("Menu item status updated successfully for ID: {}", id);
        return menuItemMapper.toDTO(savedMenuItem);
    }

    // Delete menu item (only by restaurant owner)
    public void deleteMenuItem(Long id, Long ownerId) {
        logger.info("Deleting menu item with ID: {} by owner: {}", id, ownerId);

        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantOwnerId(id, ownerId)
                .orElseThrow(() -> new UnauthorizedException("You can only delete menu items for your own restaurants"));

        menuItemRepository.delete(menuItem);
        logger.info("Menu item deleted successfully with ID: {}", id);
    }

    // Search menu items by name within a restaurant
    @Transactional(readOnly = true)
    public List<MenuItemDTO> searchMenuItemsByName(Long restaurantId, String name) {
        logger.info("Searching menu items in restaurant: {} by name: {}", restaurantId, name);
        List<MenuItem> menuItems = menuItemRepository.searchByNameInRestaurant(restaurantId, name);
        return menuItems.stream()
                .map(menuItemMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Circuit breaker for menu items
    @CircuitBreaker(name = "menu-service", fallbackMethod = "fallbackGetMenuItems")
    public List<MenuItemDTO> getMenuItemsWithCircuitBreaker(Long restaurantId) {
        return getMenuItemsByRestaurant(restaurantId);
    }

    // Fallback method for circuit breaker
    public List<MenuItemDTO> fallbackGetMenuItems(Long restaurantId, Exception ex) {
        logger.warn("Circuit breaker activated for getMenuItems: {}", ex.getMessage());
        return List.of(); // Return empty list as fallback
    }
}
