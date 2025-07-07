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
     * The queue name should match the one declared in Order Service's RabbitMQConfig
     * and bound to the 'order.event.placed' routing key.
     * @param event The OrderPlacedEvent consumed from RabbitMQ.
     */
    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_QUEUE) // Listen to the restaurant queue
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        logger.info("Restaurant Service: Received OrderPlacedEvent for Order ID: {} for restaurant: {}", event.getOrderId(), event.getRestaurantId());
        logger.info("Initiating preparation for order ID: {} at restaurant: {}", event.getOrderId(), event.getRestaurantName());
        try {
            Thread.sleep(1000);
            logger.info("Preparation simulation for Order ID: {} completed.", event.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Order preparation interrupted for Order ID: {}", event.getOrderId(), e);
        }
    }
}

