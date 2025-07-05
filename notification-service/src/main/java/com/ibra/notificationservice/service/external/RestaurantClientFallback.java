package com.ibra.notificationservice.service.external;

import com.ibra.notificationservice.dto.RestaurantDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component // Ensure this is a Spring Component
public class RestaurantClientFallback implements RestaurantClient {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantClientFallback.class);

    @Override
    public RestaurantDTO getRestaurantById(Long id) {
        logger.warn("Restaurant service is unavailable, using fallback for restaurant ID: {}", id);

        RestaurantDTO fallbackRestaurant = new RestaurantDTO();
        fallbackRestaurant.setId(id);
        fallbackRestaurant.setName("Restaurant Service Unavailable");
        fallbackRestaurant.setOwnerEmail("noreply@bytebites.com"); // Use email field
        fallbackRestaurant.setOwnerName("System Admin");

        return fallbackRestaurant;
    }
}
    