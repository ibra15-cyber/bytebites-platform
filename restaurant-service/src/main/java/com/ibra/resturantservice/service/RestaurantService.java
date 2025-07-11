package com.ibra.resturantservice.service;

import com.ibra.exception.BusinessException;
import com.ibra.exception.ResourceNotFoundException;
import com.ibra.exception.UnauthorizedException;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.dto.RestaurantDTO;
import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.enums.RestaurantStatus;
import com.ibra.resturantservice.mapper.RestaurantMapper;
import com.ibra.resturantservice.respository.RestaurantRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RestaurantService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;

    private final RestaurantMapper restaurantMapper;

    public RestaurantService(RestaurantRepository restaurantRepository, RestaurantMapper restaurantMapper) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantMapper = restaurantMapper;
    }

    public RestaurantDTO createRestaurant(CreateRestaurantRequest request, Long ownerId) {
        logger.info("Creating new restaurant for owner: {}", ownerId);

        // FIX: Removed global email uniqueness check to allow multiple restaurants per owner
        // If each *restaurant* must still have a unique email globally, re-add this check.
        // If email should be unique *per owner's restaurants*, a more complex query is needed.
        // For now, assuming an owner can use the same email for multiple restaurants.
        // if (restaurantRepository.existsByEmail(request.getEmail())) {
        //     throw new BusinessException("Restaurant with this email already exists");
        // }

        // Validate unique phone (assuming each physical restaurant location needs a unique phone)
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
        restaurant.setCreatedAt(LocalDateTime.now()); // Ensure timestamps are set
        restaurant.setUpdatedAt(LocalDateTime.now()); // Ensure timestamps are set

        logger.info("Before saving, restaurant entity details: Name={}, OwnerId={}", restaurant.getName(), restaurant.getOwnerId());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        logger.info("Restaurant created successfully with ID: {}, saved ownerId: {}", savedRestaurant.getId(), savedRestaurant.getOwnerId());

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
        List<Restaurant> restaurants = restaurantRepository.findByStatus(RestaurantStatus.ACTIVE);
        logger.info("Found {} active restaurants.", restaurants.size());
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get restaurant by ID (public endpoint)
    @Transactional(readOnly = true)
    public RestaurantDTO getRestaurantById(Long id) {
        logger.info("Fetching restaurant by ID: {}", id);
        return restaurantRepository.findById(id)
                .map(restaurantMapper::toDTO)
                .orElseThrow(() -> {
                    logger.warn("Restaurant not found with ID: {}", id);
                    return new ResourceNotFoundException("Restaurant not found with ID: " + id);
                });
    }

    // FIX: Get restaurants owned by a specific user (now returns a List<RestaurantDTO>)
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getRestaurantsByOwner(Long ownerId) {
        logger.info("Fetching all restaurants for owner ID: {}", ownerId);
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(ownerId);
        // No longer throwing ResourceNotFoundException if empty, as returning an empty list is standard for "get all"
        logger.info("Found {} restaurants for owner ID: {}", restaurants.size(), ownerId);
        return restaurants.stream()
                .map(restaurantMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Update restaurant (only by owner)
    public RestaurantDTO updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId) {
        logger.info("Updating restaurant with ID: {} by owner: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    logger.warn("Update failed: Restaurant not found with ID: {} for owner: {}", id, ownerId);
                    return new UnauthorizedException("You can only update your own restaurants, or restaurant not found.");
                });

        // FIX: Removed global email uniqueness check for updates as well
        // If email should be unique per restaurant *globally*, re-add this check.
        // if (!restaurant.getEmail().equals(request.getEmail()) &&
        //         restaurantRepository.existsByEmail(request.getEmail())) {
        //     throw new BusinessException("Restaurant with this email already exists");
        // }

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
        restaurant.setUpdatedAt(LocalDateTime.now()); // Ensure updatedAt is set

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
        restaurant.setUpdatedAt(LocalDateTime.now()); // Ensure updatedAt is set
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        logger.info("Restaurant status updated successfully for ID: {}", id);
        return restaurantMapper.toDTO(savedRestaurant);
    }

    // Delete restaurant (only by owner)
    public void deleteRestaurant(Long id, Long ownerId) {
        logger.info("Deleting restaurant with ID: {} by owner: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> {
                    logger.warn("Delete failed: Owner {} does not own restaurant {}", ownerId, id);
                    return new UnauthorizedException("You can only delete your own restaurants");
                });

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

    // Validate restaurant ownership - This method is now less critical if findByIdAndOwnerId is used directly
    // but can remain for other specific validation needs.
    public void validateRestaurantOwnership(Long restaurantId, Long ownerId) {
        logger.info("Validating ownership for restaurant ID: {} by owner: {}", restaurantId, ownerId);
        Optional<Restaurant> restaurantOptional = restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId);
        if (!restaurantOptional.isPresent()) {
            logger.warn("Unauthorized attempt: Owner {} does not own restaurant {}", ownerId, restaurantId);
            throw new UnauthorizedException("You don't own this restaurant");
        }
        logger.info("Ownership validated for restaurant ID: {} by owner: {}", restaurantId, ownerId);
    }

    @CircuitBreaker(name = "restaurant-service", fallbackMethod = "fallbackGetAllRestaurants")
    public List<RestaurantDTO> getRestaurantsWithCircuitBreaker() {
        return getAllActiveRestaurants();
    }

    public List<RestaurantDTO> fallbackGetAllRestaurants(Exception ex) {
        logger.warn("Circuit breaker activated for getAllRestaurants: {}", ex.getMessage());
        return List.of(); // Return empty list as fallback
    }
}
