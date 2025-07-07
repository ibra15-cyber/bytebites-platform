package com.ibra.resturantservice.service.rabbitmq;

import com.ibra.resturantservice.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPreparationListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPreparationListener.class);

    /**
     * Listens for OrderPlacedEvent messages on the restaurant preparation queue.
     * The queue name is defined in this service's own RabbitMQConfig.
     * @param event The OrderPlacedEvent consumed from RabbitMQ.
     */
    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_QUEUE) // Correctly references its own config
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        logger.info("Restaurant Service: Received OrderPlacedEvent for Order ID: {} for restaurant: {}", event.getOrderId(), event.getRestaurantId());
        logger.info("Initiating preparation for order ID: {} at restaurant: {}", event.getOrderId(), event.getRestaurantName());

        // In a real application, you would:
        // 1. Update order status in the restaurant's local database (e.g., to "PREPARING")
        // 2. Trigger internal kitchen management systems/APIs
        // 3. Potentially publish new events (e.g., "OrderPreparationStartedEvent")
        // 4. Update the local view of the order for the restaurant's dashboard

        // Simulate preparation time
        try {
            Thread.sleep(1000); // Simulate some processing time
            logger.info("Preparation simulation for Order ID: {} completed.", event.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Order preparation interrupted for Order ID: {}", event.getOrderId(), e);
        }
        logger.info("Order prepared: {}, {}, {} ", event.getOrderId(), event.getRestaurantName(), event.getCustomerEmail());
    }
}