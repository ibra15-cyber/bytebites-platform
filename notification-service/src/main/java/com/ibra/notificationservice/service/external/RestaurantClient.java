package com.ibra.notificationservice.service.external;


import com.ibra.notificationservice.dto.RestaurantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", fallback = RestaurantClientFallback.class)
public interface RestaurantClient {

    @GetMapping("/api/restaurants/{id}")
    RestaurantDTO getRestaurantById(@PathVariable("id") Long id);
}
