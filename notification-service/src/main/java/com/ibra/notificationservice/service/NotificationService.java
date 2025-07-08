package com.ibra.notificationservice.service;

import com.ibra.dto.OrderPlacedEvent;
import com.ibra.dto.RestaurantDTO;
import com.ibra.notificationservice.service.email.EmailService;
import com.ibra.notificationservice.service.external.RestaurantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final RestaurantClient restaurantClient; // Autowire Feign client

    @Autowired
    public NotificationService(EmailService emailService, RestaurantClient restaurantClient) {
        this.emailService = emailService;
        this.restaurantClient = restaurantClient;
    }

    /**
     * Processes an OrderPlacedEvent to send notifications to customer and restaurant.
     * @param event The OrderPlacedEvent received from RabbitMQ.
     */
    public void processOrderEvent(OrderPlacedEvent event) {
        logger.info("Processing order event type: {} for order ID: {}", event.getEventType(), event.getOrderId());

        // 1. Send customer notification (always attempt)
        emailService.sendCustomerOrderConfirmation(event); // Method name might be more generic now

        // 2. Fetch Restaurant details for restaurant notification
        RestaurantDTO restaurant = null;
        try {
            // Use the Feign client to get restaurant details
            // Note: RestaurantClient.getRestaurantById() might return RestaurantDto directly,
            // or an ApiResponse<RestaurantDto> depending on your Feign client's definition.
            // Adjust if your RestaurantClient returns ApiResponse.
            restaurant = restaurantClient.getRestaurantById(event.getRestaurantId());

            if (restaurant == null || restaurant.getId() == null) {
                logger.warn("Could not fetch restaurant details for ID: {}. Skipping restaurant notification.", event.getRestaurantId());
            } else {
                // 3. Send restaurant notification
                emailService.sendRestaurantOrderNotification(event, restaurant);
            }
        } catch (Exception e) {
            logger.error("Error fetching restaurant details for ID: {} or sending restaurant email for order {}: {}",
                    event.getRestaurantId(), event.getOrderId(), e.getMessage(), e);
            // The fallback in RestaurantClientFallback should handle this gracefully
        }
    }
}
    