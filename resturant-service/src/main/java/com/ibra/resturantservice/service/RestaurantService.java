package com.ibra.resturantservice.service;


import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.resturantservice.dto.RestaurantDTO;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.resturantservice.enums.RestaurantStatus;
import com.ibra.resturantservice.exception.BusinessException;
import com.ibra.resturantservice.exception.ResourceNotFoundException;
import com.ibra.resturantservice.exception.UnauthorizedException;
import com.ibra.resturantservice.mapper.RestaurantMapper;
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
public class RestaurantService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantService.class);

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantMapper restaurantMapper;

    public RestaurantDTO createRestaurant(CreateRestaurantRequest request, Long ownerId) {
        logger.info("Creating new restaurant for owner: {}", ownerId);

        // Validate unique email and phone
        if (restaurantRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Restaurant with this email already exists");
        }

        if (restaurantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Restaurant with this phone number already exists");
        }

        Restaurant restaurant = new Restaurant(
                request.getName(),
                request.getDescription(),
                request.getAddress(),
                request.getPhoneNumber(),
                request.getEmail(),
                ownerId
        );

        restaurant.setImageUrl(request.getImageUrl());
        restaurant.setStatus(RestaurantStatus.PENDING_APPROVAL);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        logger.info("Restaurant created successfully with ID: {}", savedRestaurant.getId());

        return restaurantMapper.toDTO(savedRestaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantDTO> getAllRestaurants() {
        logger.info("Admin fetching all restaurants (including inactive)");
        List<Restaurant> restaurants = restaurantRepository.findAll();
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get all active restaurants (public endpoint)
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getAllActiveRestaurants() {
        logger.info("Fetching all active restaurants");
        List<Restaurant> restaurants = restaurantRepository.findActiveRestaurants();
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get restaurant by ID (public endpoint)
    @Transactional(readOnly = true)
    public RestaurantDTO getRestaurantById(Long id) {
        logger.info("Fetching restaurant with ID: {}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + id));

        return restaurantMapper.toDTO(restaurant);
    }

    // Get restaurants owned by a specific user
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getRestaurantsByOwner(Long ownerId) {
        logger.info("Fetching restaurants for owner: {}", ownerId);
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(ownerId);
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Update restaurant (only by owner)
    public RestaurantDTO updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId) {
        logger.info("Updating restaurant with ID: {} by owner: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new UnauthorizedException("You can only update your own restaurants"));

        // Check for email uniqueness if changing email
        if (!restaurant.getEmail().equals(request.getEmail()) &&
                restaurantRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Restaurant with this email already exists");
        }

        // Check for phone uniqueness if changing phone
        if (!restaurant.getPhoneNumber().equals(request.getPhoneNumber()) &&
                restaurantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Restaurant with this phone number already exists");
        }

        // Update fields
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhoneNumber(request.getPhoneNumber());
        restaurant.setEmail(request.getEmail());
        restaurant.setImageUrl(request.getImageUrl());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        logger.info("Restaurant updated successfully with ID: {}", savedRestaurant.getId());

        return restaurantMapper.toDTO(savedRestaurant);
    }

    // Update restaurant status (admin only)
    public RestaurantDTO updateRestaurantStatus(Long id, RestaurantStatus status) {
        logger.info("Updating restaurant status for ID: {} to: {}", id, status);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + id));

        restaurant.setStatus(status);
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        logger.info("Restaurant status updated successfully for ID: {}", id);
        return restaurantMapper.toDTO(savedRestaurant);
    }

    // Delete restaurant (only by owner)
    public void deleteRestaurant(Long id, Long ownerId) {
        logger.info("Deleting restaurant with ID: {} by owner: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new UnauthorizedException("You can only delete your own restaurants"));

        restaurantRepository.delete(restaurant);
        logger.info("Restaurant deleted successfully with ID: {}", id);
    }

    // Search restaurants by name
    @Transactional(readOnly = true)
    public List<RestaurantDTO> searchRestaurantsByName(String name) {
        logger.info("Searching restaurants by name: {}", name);
        List<Restaurant> restaurants = restaurantRepository.searchByName(name);
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Search restaurants by address
    @Transactional(readOnly = true)
    public List<RestaurantDTO> searchRestaurantsByAddress(String address) {
        logger.info("Searching restaurants by address: {}", address);
        List<Restaurant> restaurants = restaurantRepository.searchByAddress(address);
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Validate restaurant ownership
    public void validateRestaurantOwnership(Long restaurantId, Long ownerId) {
        if (!restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId).isPresent()) {
            throw new UnauthorizedException("You don't own this restaurant");
        }
    }

    // Circuit breaker fallback method
    @CircuitBreaker(name = "restaurant-service", fallbackMethod = "fallbackGetAllRestaurants")
    public List<RestaurantDTO> getRestaurantsWithCircuitBreaker() {
        return getAllActiveRestaurants();
    }

    // Fallback method for circuit breaker
    public List<RestaurantDTO> fallbackGetAllRestaurants(Exception ex) {
        logger.warn("Circuit breaker activated for getAllRestaurants: {}", ex.getMessage());
        return List.of(); // Return empty list as fallback
    }
}
