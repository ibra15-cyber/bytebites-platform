package com.ibra.orderservice.service.external;

import com.ibra.dto.ApiResponse;
import com.ibra.dto.MenuItemDTO;
import com.ibra.dto.RestaurantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "restaurant-service", configuration = FeignClientProperties.FeignClientConfiguration.class)
public interface RestaurantServiceClient {

    @GetMapping("/api/restaurants/{id}")
    ApiResponse<RestaurantDTO> getRestaurantById(@PathVariable("id") Long id);

    @GetMapping("/api/menu-items/{id}")
    ApiResponse<MenuItemDTO> getMenuItemById(@PathVariable("id") Long id);

    @GetMapping("/api/restaurants/owner/{ownerId}")
    ApiResponse<RestaurantDTO> getRestaurantByOwnerId(@PathVariable("ownerId") Long ownerId);

    @GetMapping("/api/menu-items/restaurants/{restaurantId}")
    ApiResponse<List<MenuItemDTO>> getMenuItemsByRestaurant(@PathVariable("restaurantId") Long restaurantId);

    @GetMapping("/api/menu-items/restaurants/{restaurantId}/category/{category}")
    ApiResponse<List<MenuItemDTO>> getMenuItemsByCategory(
            @PathVariable("restaurantId") Long restaurantId,
            @PathVariable("category") String category);
}