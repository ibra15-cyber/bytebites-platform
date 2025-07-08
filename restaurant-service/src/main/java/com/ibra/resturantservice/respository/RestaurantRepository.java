package com.ibra.resturantservice.respository;


import com.ibra.resturantservice.entity.Restaurant;
import com.ibra.enums.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // Find restaurants by owner ID
    List<Restaurant> findByOwnerId(Long ownerId);

    // Find restaurants by status
    List<Restaurant> findByStatus(RestaurantStatus status);

    // Find active restaurants
    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE'")
    List<Restaurant> findActiveRestaurants();

    // Search restaurants by name (case-insensitive)
    @Query("SELECT r FROM Restaurant r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) AND r.status = 'ACTIVE'")
    List<Restaurant> searchByName(@Param("name") String name);

    // Search restaurants by address (case-insensitive)
    @Query("SELECT r FROM Restaurant r WHERE LOWER(r.address) LIKE LOWER(CONCAT('%', :address, '%')) AND r.status = 'ACTIVE'")
    List<Restaurant> searchByAddress(@Param("address") String address);

    // Find restaurant by owner ID and restaurant ID (for ownership validation)
    Optional<Restaurant> findByIdAndOwnerId(Long id, Long ownerId);

    // Check if restaurant exists by email
    boolean existsByEmail(String email);

    // Check if restaurant exists by phone number
    boolean existsByPhoneNumber(String phoneNumber);
}
