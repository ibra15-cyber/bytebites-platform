package com.ibra.orderservice.service.rabbitmq;

import com.ibra.orderservice.dto.OrderPlacedEvent;
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

    @Autowired
    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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
                    event.getOrderId(), RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_PLACED);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_PLACED, event);
            logger.info("ORDER_PLACED event published successfully for Order ID: {}", event.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_PLACED event for order: {}", order.getId(), e);
        }
    }

    /**
     * Publishes an ORDER_CANCELLED event to the RabbitMQ exchange.
     * @param order The Order entity to publish.
     */
    public void publishOrderCancelledEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            event.setEventType("ORDER_CANCELLED"); // Set event type

            logger.info("Publishing ORDER_CANCELLED event for order: {} to exchange: {} with routing key: {}",
                    order.getId(), RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_CANCELLED);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_CANCELLED, event);
            logger.info("ORDER_CANCELLED event published successfully for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_CANCELLED event for order: {}", order.getId(), e);
        }
    }

    /**
     * Publishes an ORDER_STATUS_UPDATED event to the RabbitMQ exchange.
     * @param order The Order entity to publish.
     */
    public void publishOrderStatusUpdatedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            event.setEventType("ORDER_STATUS_UPDATED"); // Set event type

            logger.info("Publishing ORDER_STATUS_UPDATED event for order: {} to exchange: {} with routing key: {}",
                    order.getId(), RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_STATUS_UPDATED);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_STATUS_UPDATED, event);
            logger.info("ORDER_STATUS_UPDATED event published successfully for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_STATUS_UPDATED event for order: {}", order.getId(), e);
        }
    }

    /**
     * Publishes an ORDER_RATED event to the RabbitMQ exchange.
     * @param order The Order entity to publish.
     */
    public void publishOrderRatedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            event.setEventType("ORDER_RATED"); // Set event type

            logger.info("Publishing ORDER_RATED event for order: {} to exchange: {} with routing key: {}",
                    order.getId(), RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_RATED);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY_RATED, event);
            logger.info("ORDER_RATED event published successfully for order: {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to publish ORDER_RATED event for order: {}", order.getId(), e);
        }
    }

    /**
     * Creates a standardized OrderPlacedEvent from an Order entity.
     * This helper method ensures consistency in mapping Order entity to OrderPlacedEvent DTO.
     * @param order The Order entity.
     * @return An OrderPlacedEvent DTO.
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

            event.setItems(itemDetails);
        }
        return event;
    }
}
