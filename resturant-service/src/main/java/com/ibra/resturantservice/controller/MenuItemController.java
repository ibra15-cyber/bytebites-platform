package com.ibra.resturantservice.controller;

import com.ibra.resturantservice.dto.ApiResponse;
import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.resturantservice.dto.MenuItemDTO;
import com.ibra.resturantservice.enums.MenuItemCategory;
import com.ibra.resturantservice.enums.MenuItemStatus;
import com.ibra.resturantservice.service.MenuItemService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private static final Logger logger = LoggerFactory.getLogger(MenuItemController.class);

    @Autowired
    private MenuItemService menuItemService;

    // Helper method to extract user ID from request headers
    // This assumes X-User-Id is the numerical ID, as per our last discussion for ownership validation
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            // This should ideally not happen if Gateway is correctly forwarding authenticated requests
            logger.warn("X-User-Id header is missing or empty.");
            throw new SecurityException("User ID not found in request headers.");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            logger.error("Invalid X-User-Id header format: {}", userIdHeader, e);
            throw new IllegalArgumentException("Invalid user ID format in header.");
        }
    }

    /**
     * Creates a new menu item for a specific restaurant.
     * Requires RESTAURANT_OWNER role.
     * The ownerId for validation is extracted from the X-User-Id header.
     * @param restaurantId The ID of the restaurant to add the menu item to.
     * @param request The request body containing menu item details.
     * @param httpRequest The HttpServletRequest to extract X-User-Id from.
     * @return ResponseEntity with the created MenuItemDTO.
     */
    @PostMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<MenuItemDTO>> createMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Received request to create menu item for restaurant ID: {}", restaurantId);
        Long ownerId = getUserIdFromRequest(httpRequest);
        MenuItemDTO createdMenuItem = menuItemService.createMenuItem(restaurantId, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Menu item created successfully", createdMenuItem));
    }

    /**
     * Retrieves all available menu items for a specific restaurant.
     * Publicly accessible (no authentication required).
     * @param restaurantId The ID of the restaurant.
     * @return ResponseEntity with a list of MenuItemDTOs.
     */
    @GetMapping("/restaurants/{restaurantId}")
    @CircuitBreaker(name = "menu-service", fallbackMethod = "fallbackGetMenuItemsForRestaurant")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> getMenuItemsByRestaurant(
            @PathVariable Long restaurantId) {
        logger.info("Received request to get menu items for restaurant ID: {}", restaurantId);
        List<MenuItemDTO> menuItems = menuItemService.getMenuItemsByRestaurant(restaurantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu items fetched successfully", menuItems));
    }

    /**
     * Fallback method for getMenuItemsByRestaurant (Circuit Breaker).
     */
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> fallbackGetMenuItemsForRestaurant(
            Long restaurantId, Exception ex) {
        logger.warn("Fallback triggered for getMenuItemsByRestaurant for restaurant ID: {}. Reason: {}", restaurantId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, "Service unavailable. Could not fetch menu items for restaurant " + restaurantId, List.of()));
    }

    /**
     * Retrieves menu items for a specific restaurant by category.
     * Publicly accessible (no authentication required).
     * @param restaurantId The ID of the restaurant.
     * @param category The category of menu items.
     * @return ResponseEntity with a list of MenuItemDTOs.
     */
    @GetMapping("/restaurants/{restaurantId}/category")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> getMenuItemsByCategory(
            @PathVariable Long restaurantId,
            @RequestParam MenuItemCategory category) {
        logger.info("Received request to get menu items for restaurant ID: {} by category: {}", restaurantId, category);
        List<MenuItemDTO> menuItems = menuItemService.getMenuItemsByCategory(restaurantId, category);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu items fetched successfully by category", menuItems));
    }

    /**
     * Retrieves a single menu item by its ID.
     * Publicly accessible (no authentication required).
     * @param id The ID of the menu item.
     * @return ResponseEntity with the MenuItemDTO.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemDTO>> getMenuItemById(@PathVariable Long id) {
        logger.info("Received request to get menu item by ID: {}", id);
        MenuItemDTO menuItem = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item fetched successfully", menuItem));
    }

    /**
     * Updates an existing menu item.
     * Requires RESTAURANT_OWNER role.
     * Ownership is validated in the service layer using the ownerId from the X-User-Id header.
     * @param id The ID of the menu item to update.
     * @param request The request body containing updated menu item details.
     * @param httpRequest The HttpServletRequest to extract X-User-Id from.
     * @return ResponseEntity with the updated MenuItemDTO.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<MenuItemDTO>> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateMenuItemRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Received request to update menu item ID: {}", id);
        Long ownerId = getUserIdFromRequest(httpRequest);
        MenuItemDTO updatedMenuItem = menuItemService.updateMenuItem(id, request, ownerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item updated successfully", updatedMenuItem));
    }

    /**
     * Updates the status of a menu item.
     * Requires RESTAURANT_OWNER role.
     * Ownership is validated in the service layer using the ownerId from the X-User-Id header.
     * @param id The ID of the menu item to update status for.
     * @param status The new status for the menu item.
     * @param httpRequest The HttpServletRequest to extract X-User-Id from.
     * @return ResponseEntity with the updated MenuItemDTO.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<MenuItemDTO>> updateMenuItemStatus(
            @PathVariable Long id,
            @RequestParam MenuItemStatus status,
            HttpServletRequest httpRequest) {
        logger.info("Received request to update menu item ID: {} status to {}", id, status);
        Long ownerId = getUserIdFromRequest(httpRequest);
        MenuItemDTO updatedMenuItem = menuItemService.updateMenuItemStatus(id, status, ownerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item status updated successfully", updatedMenuItem));
    }

    /**
     * Deletes a menu item.
     * Requires RESTAURANT_OWNER role.
     * Ownership is validated in the service layer using the ownerId from the X-User-Id header.
     * @param id The ID of the menu item to delete.
     * @param httpRequest The HttpServletRequest to extract X-User-Id from.
     * @return ResponseEntity indicating success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        logger.info("Received request to delete menu item ID: {}", id);
        Long ownerId = getUserIdFromRequest(httpRequest);
        menuItemService.deleteMenuItem(id, ownerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu item deleted successfully", null));
    }

    /**
     * Searches for menu items by name within a specific restaurant.
     * Publicly accessible (no authentication required).
     * @param restaurantId The ID of the restaurant.
     * @param name The name (or part of the name) to search for.
     * @return ResponseEntity with a list of MenuItemDTOs.
     */
    @GetMapping("/restaurants/{restaurantId}/search")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> searchMenuItemsByName(
            @PathVariable Long restaurantId,
            @RequestParam String name) {
        logger.info("Received request to search menu items by name: {} in restaurant ID: {}", name, restaurantId);
        List<MenuItemDTO> menuItems = menuItemService.searchMenuItemsByName(restaurantId, name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menu items searched successfully", menuItems));
    }
}

