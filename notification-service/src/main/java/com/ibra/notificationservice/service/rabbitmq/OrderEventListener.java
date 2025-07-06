package com.ibra.notificationservice.service.rabbitmq;

import com.ibra.notificationservice.dto.OrderPlacedEvent;
import com.ibra.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService; // Autowire the new service

    @Value("${app.rabbitmq.notification-queue-name:notification.order.queue}") // Default value if not found
    private String notificationQueueName;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Listens for OrderPlacedEvent messages on the notification queue.
     * The queue name should match the one declared in RabbitMQConfig (in order-service)
     * and configured in this service's application.yml.
     * @param event The OrderPlacedEvent consumed from RabbitMQ.
     */
    @RabbitListener(queues = "${app.rabbitmq.notification-queue-name:notification.order.queue}") // Use SpEL to read from property
    public void handleOrderEvent(OrderPlacedEvent event) {
        logger.info("Notification Service: Received order event: {} for order: {}", event.getEventType(), event.getOrderId());

        try {
            // Process the order event using the NotificationService
            notificationService.processOrderEvent(event);

            logger.info("Successfully handled order event: {} for order: {}",
                    event.getEventType(), event.getOrderId());

        } catch (Exception e) {
            logger.error("Error handling order event: {} for order: {}: {}",
                    event.getEventType(), event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }
}
