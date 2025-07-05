// RestaurantController.java
package com.ibra.resturantservice.controller;

import com.ibra.resturantservice.dto.ApiResponse;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.resturantservice.dto.RestaurantDTO;
import com.ibra.resturantservice.enums.RestaurantStatus;
import com.ibra.resturantservice.service.RestaurantService;
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
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantController.class);

    @Autowired
    private RestaurantService restaurantService;

    // Public endpoint - Get all active restaurants
    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> getAllRestaurants() {
        logger.info("Fetching all active restaurants");
        List<RestaurantDTO> restaurants = restaurantService.getAllActiveRestaurants();
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants fetched successfully", restaurants));
    }

    // Public endpoint - Get restaurant by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantDTO>> getRestaurantById(@PathVariable Long id) {
        logger.info("Fetching restaurant with ID: {}", id);
        RestaurantDTO restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant fetched successfully", restaurant));
    }

    // Public endpoint - Search restaurants by name
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> searchRestaurants(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address) {

        logger.info("Searching restaurants with name: {} and address: {}", name, address);

        List<RestaurantDTO> restaurants;
        if (name != null && !name.trim().isEmpty()) {
            restaurants = restaurantService.searchRestaurantsByName(name);
        } else if (address != null && !address.trim().isEmpty()) {
            restaurants = restaurantService.searchRestaurantsByAddress(address);
        } else {
            restaurants = restaurantService.getAllActiveRestaurants();
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants searched successfully", restaurants));
    }

    // Restaurant Owner only - Create new restaurant
    @PostMapping
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<RestaurantDTO>> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        logger.info("Creating new restaurant for owner: {}", userId);
        RestaurantDTO restaurant = restaurantService.createRestaurant(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Restaurant created successfully", restaurant));
    }

    // Restaurant Owner only - Get own restaurants
    @GetMapping("/my-restaurants")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> getMyRestaurants(
            @RequestHeader("X-User-Id") Long userId) {

        logger.info("Fetching restaurants for owner: {}", userId);
        List<RestaurantDTO> restaurants = restaurantService.getRestaurantsByOwner(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Owner restaurants fetched successfully", restaurants));
    }

    // Restaurant Owner only - Update own restaurant
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<RestaurantDTO>> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody CreateRestaurantRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        logger.info("Updating restaurant with ID: {} by owner: {}", id, userId);
        RestaurantDTO restaurant = restaurantService.updateRestaurant(id, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant updated successfully", restaurant));
    }

    // Restaurant Owner only - Delete own restaurant
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        logger.info("Deleting restaurant with ID: {} by owner: {}", id, userId);
        restaurantService.deleteRestaurant(id, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant deleted successfully", null));
    }

    // Admin only - Update restaurant status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<RestaurantDTO>> updateRestaurantStatus(
            @PathVariable Long id,
            @RequestParam RestaurantStatus status) {

        logger.info("Admin updating restaurant status for ID: {} to: {}", id, status);
        RestaurantDTO restaurant = restaurantService.updateRestaurantStatus(id, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant status updated successfully", restaurant));
    }

    // Admin only - Get all restaurants (including inactive)
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> getAllRestaurantsForAdmin() {
        logger.info("Admin fetching all restaurants");
        List<RestaurantDTO> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(new ApiResponse<>(true, "All restaurants fetched successfully", restaurants));
    }

    // Authenticated users - Get restaurants with circuit breaker
    @GetMapping("/with-circuit-breaker")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RestaurantDTO>>> getRestaurantsWithCircuitBreaker() {
        logger.info("Fetching restaurants with circuit breaker");
        List<RestaurantDTO> restaurants = restaurantService.getRestaurantsWithCircuitBreaker();
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants fetched with circuit breaker", restaurants));
    }

    // Public endpoint - Get restaurant by owner ID (if you still need this)
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<ApiResponse<RestaurantDTO>> getRestaurantByOwnerId(@PathVariable Long ownerId) {
        logger.info("Fetching restaurant for owner ID: {}", ownerId);
        // You'll need to implement this method in your service if it doesn't exist
        // For now, I'll assume you have a method that returns a single restaurant for an owner
        List<RestaurantDTO> restaurants = restaurantService.getRestaurantsByOwner(ownerId);
        if (restaurants.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(false, "No restaurant found for owner", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant fetched successfully", restaurants.get(0)));
    }
}