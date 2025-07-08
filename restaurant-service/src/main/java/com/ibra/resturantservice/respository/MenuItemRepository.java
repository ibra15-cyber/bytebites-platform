package com.ibra.resturantservice.respository;

import com.ibra.resturantservice.entity.MenuItem;
import com.ibra.enums.MenuItemCategory;
import com.ibra.enums.MenuItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    // Add @EntityGraph to ensure the 'restaurant' association is eagerly fetched
    // when finding a MenuItem by its ID.
//    @EntityGraph(attributePaths = "restaurant")
//    Optional<MenuItem> findById(@NonNull Long id);
//
    // Find menu items by restaurant ID
    // Consider adding @EntityGraph here as well if you need restaurant details with these fetches
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    List<MenuItem> findByRestaurantId(Long restaurantId);

    // Find available menu items by restaurant ID
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    List<MenuItem> findByRestaurantIdAndStatus(Long restaurantId, MenuItemStatus status);


    // Find menu items by category and restaurant
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    List<MenuItem> findByRestaurantIdAndCategory(Long restaurantId, MenuItemCategory category);

    // Find available menu items by category and restaurant
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.category = :category AND m.status = 'AVAILABLE'")
    List<MenuItem> findAvailableItemsByRestaurantAndCategory(@Param("restaurantId") Long restaurantId, @Param("category") MenuItemCategory category);

    // Search menu items by name within a restaurant
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) AND m.status = 'AVAILABLE'")
    List<MenuItem> searchByNameInRestaurant(@Param("restaurantId") Long restaurantId, @Param("name") String name);

    // Find menu item by ID and restaurant ID (for ownership validation)
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    Optional<MenuItem> findByIdAndRestaurantId(Long id, Long restaurantId);

    // Find menu item by ID and restaurant owner ID (for ownership validation)
    // This query already explicitly joins to m.restaurant, but @EntityGraph ensures the object is fully initialized.
    @EntityGraph(attributePaths = "restaurant") // Added for consistency
    @Query("SELECT m FROM MenuItem m WHERE m.id = :itemId AND m.restaurant.ownerId = :ownerId")
    Optional<MenuItem> findByIdAndRestaurantOwnerId(@Param("itemId") Long itemId, @Param("ownerId") Long ownerId);

    // Count menu items by restaurant
    long countByRestaurantId(Long restaurantId);

    // Count available menu items by restaurant
    long countByRestaurantIdAndStatus(Long restaurantId, MenuItemStatus status);
}
