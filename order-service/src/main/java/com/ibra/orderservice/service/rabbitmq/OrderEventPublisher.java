
package com.ibra.orderservice.service.rabbitmq;

import com.ibra.dto.OrderPlacedEvent;
import com.ibra.orderservice.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig; // Inject RabbitMQConfig

    @Autowired
    public OrderEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQConfig rabbitMQConfig) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQConfig = rabbitMQConfig; // Assign injected config
    }

    /**
     * Publishes an ORDER_PLACED event to the RabbitMQ exchange.
     * This method maps the Order entity to the OrderPlacedEvent DTO.
     * @param order The Order entity to publish.
     */
    public void publishOrderPlacedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            event.setEventType("ORDER_PLACED"); // Ensure eventType is set for placed orders

            logger.info("Publishing ORDER_PLACED event for Order ID: {} to exchange: {} with routing key: {}",
                    event.getOrderId(), rabbitMQConfig.getOrderExchangeName(), rabbitMQConfig.getOrderRoutingKeyPlaced()); // Use injected values

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getOrderExchangeName(), // Use injected value
                    rabbitMQConfig.getOrderRoutingKeyPlaced(), // Use injected value
                    event
            );
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_PLACED event for Order ID: {}", order.getId(), e);
            // Handle error, e.g., retry, dead-letter queue, or specific exception
        }
    }

    /**
     * Publishes an ORDER_STATUS_UPDATED event to the RabbitMQ exchange.
     * This method maps the Order entity to the OrderPlacedEvent DTO (reusing for status updates).
     * @param order The Order entity with updated status.
     */
    public void publishOrderStatusUpdatedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order); // Reuse conversion logic
            event.setEventType("ORDER_STATUS_UPDATED"); // Set event type

            logger.info("Publishing ORDER_STATUS_UPDATED event for Order ID: {} to exchange: {} with routing key: {}",
                    event.getOrderId(), rabbitMQConfig.getOrderExchangeName(), rabbitMQConfig.getOrderRoutingKeyStatusUpdated());

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getOrderExchangeName(),
                    rabbitMQConfig.getOrderRoutingKeyStatusUpdated(),
                    event
            );
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_STATUS_UPDATED event for Order ID: {}", order.getId(), e);
        }
    }

    /**
     * Publishes an ORDER_CANCELLED event to the RabbitMQ exchange.
     * @param order The Order entity that was cancelled.
     */
    public void publishOrderCancelledEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order); // Reuse conversion logic
            event.setEventType("ORDER_CANCELLED"); // Set event type

            logger.info("Publishing ORDER_CANCELLED event for Order ID: {} to exchange: {} with routing key: {}",
                    event.getOrderId(), rabbitMQConfig.getOrderExchangeName(), rabbitMQConfig.getOrderRoutingKeyCancelled());

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getOrderExchangeName(),
                    rabbitMQConfig.getOrderRoutingKeyCancelled(),
                    event
            );
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_CANCELLED event for Order ID: {}", order.getId(), e);
        }
    }

    /**
     * Publishes an ORDER_RATED event to the RabbitMQ exchange.
     * @param order The Order entity that was rated.
     */
    public void publishOrderRatedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order); // Reuse conversion logic
            event.setEventType("ORDER_RATED"); // Set event type

            logger.info("Publishing ORDER_RATED event for Order ID: {} to exchange: {} with routing key: {}",
                    event.getOrderId(), rabbitMQConfig.getOrderExchangeName(), rabbitMQConfig.getOrderRoutingKeyRated());

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getOrderExchangeName(),
                    rabbitMQConfig.getOrderRoutingKeyRated(),
                    event
            );
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_RATED event for Order ID: {}", order.getId(), e);
        }
    }


    /**
     * Helper method to convert an Order entity to an OrderPlacedEvent DTO.
     * This is a simplified conversion. In a real application, you might use MapStruct.
     */
    private OrderPlacedEvent createOrderPlacedEvent(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderId(order.getId());
        event.setCustomerId(order.getCustomerId());
        event.setRestaurantId(order.getRestaurantId());
        event.setRestaurantName(order.getRestaurantName());
        event.setTotalAmount(BigDecimal.valueOf(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0));
        event.setOrderTime(order.getCreatedAt()); // Assuming createdAt is the orderTime
        event.setCustomerEmail(order.getCustomerEmail());
        event.setDeliveryAddress(order.getDeliveryAddress());
        event.setDeliveryPhone(order.getDeliveryPhone());
        event.setStatus(order.getStatus().name()); // Convert enum to String

        // Convert order items to event details using the shared OrderItemEvent DTO
        if (order.getOrderItems() != null) {
            List<OrderPlacedEvent.OrderItemEvent> itemDetails = order.getOrderItems().stream()
                    .map(orderItem -> new OrderPlacedEvent.OrderItemEvent(
                            orderItem.getMenuItemId(),
                            orderItem.getMenuItemName(),
                            orderItem.getQuantity(),
                            orderItem.getUnitPrice()
                    )).collect(Collectors.toList());

            event.setOrderItems(itemDetails);
        }
        return event;
    }
}