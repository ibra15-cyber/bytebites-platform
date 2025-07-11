//package com.ibra.orderservice.service.rabbitmq;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//
//import com.ibra.dto.OrderPlacedEvent;
//import com.ibra.orderservice.entity.Order;
//import com.ibra.orderservice.entity.OrderItem;
//import com.ibra.orderservice.enums.OrderStatus;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class OrderEventPublisherTest {
//
//    @Mock
//    private RabbitTemplate rabbitTemplate;
//
//    @InjectMocks
//    private OrderEventPublisher orderEventPublisher;
//
//    private Order testOrder;
//    private OrderItem orderItem1;
//    private OrderItem orderItem2;
//
//    @BeforeEach
//    void setUp() {
//        // Create test order items
//        orderItem1 = new OrderItem();
//        orderItem1.setMenuItemId(1L);
//        orderItem1.setMenuItemName("Burger");
//        orderItem1.setQuantity(2);
//        orderItem1.setUnitPrice(BigDecimal.valueOf(15.99));
//
//        orderItem2 = new OrderItem();
//        orderItem2.setMenuItemId(2L);
//        orderItem2.setMenuItemName("Fries");
//        orderItem2.setQuantity(1);
//        orderItem2.setUnitPrice(BigDecimal.valueOf(5.99));
//
//        // Create test order
//        testOrder = new Order();
//        testOrder.setId(123L);
//        testOrder.setCustomerId(456L);
//        testOrder.setRestaurantId(789L);
//        testOrder.setRestaurantName("Test Restaurant");
//        testOrder.setTotalAmount(BigDecimal.valueOf(37.97));
//        testOrder.setCreatedAt(LocalDateTime.now());
//        testOrder.setCustomerEmail("test@example.com");
//        testOrder.setDeliveryAddress("123 Test Street");
//        testOrder.setDeliveryPhone("123-456-7890");
//        testOrder.setStatus(OrderStatus.PENDING);
//        testOrder.setOrderItems(Arrays.asList(orderItem1, orderItem2));
//    }
//
//    @Test
//    void testPublishOrderPlacedEvent_Success() {
//        // Act
//        orderEventPublisher.publishOrderPlacedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_PLACED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertOrderPlacedEvent(capturedEvent, "ORDER_PLACED");
//    }
//
//    @Test
//    void testPublishOrderCancelledEvent_Success() {
//        // Act
//        orderEventPublisher.publishOrderCancelledEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_CANCELLED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertOrderPlacedEvent(capturedEvent, "ORDER_CANCELLED");
//    }
//
//    @Test
//    void testPublishOrderStatusUpdatedEvent_Success() {
//        // Act
//        orderEventPublisher.publishOrderStatusUpdatedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_STATUS_UPDATED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertOrderPlacedEvent(capturedEvent, "ORDER_STATUS_UPDATED");
//    }
//
//    @Test
//    void testPublishOrderRatedEvent_Success() {
//        // Act
//        orderEventPublisher.publishOrderRatedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_RATED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertOrderPlacedEvent(capturedEvent, "ORDER_RATED");
//    }
//
//    @Test
//    void testPublishOrderPlacedEvent_WithException() {
//        // Arrange
//        doThrow(new RuntimeException("RabbitMQ connection failed"))
//                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(OrderPlacedEvent.class));
//
//        // Act & Assert - should not throw exception, but log error
//        assertDoesNotThrow(() -> orderEventPublisher.publishOrderPlacedEvent(testOrder));
//
//        // Verify the method was called despite the exception
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_PLACED),
//                any(OrderPlacedEvent.class)
//        );
//    }
//
//    @Test
//    void testPublishOrderCancelledEvent_WithException() {
//        // Arrange
//        doThrow(new RuntimeException("RabbitMQ connection failed"))
//                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(OrderPlacedEvent.class));
//
//        // Act & Assert
//        assertDoesNotThrow(() -> orderEventPublisher.publishOrderCancelledEvent(testOrder));
//
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_CANCELLED),
//                any(OrderPlacedEvent.class)
//        );
//    }
//
//    @Test
//    void testCreateOrderPlacedEvent_WithNullOrderItems() {
//        // Arrange
//        testOrder.setOrderItems(null);
//
//        // Act
//        orderEventPublisher.publishOrderPlacedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_PLACED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertNull(capturedEvent.getOrderItems());
//    }
//
//    @Test
//    void testCreateOrderPlacedEvent_WithNullTotalAmount() {
//        // Arrange
//        testOrder.setTotalAmount(null);
//
//        // Act
//        orderEventPublisher.publishOrderPlacedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_PLACED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertEquals(BigDecimal.valueOf(0.0), capturedEvent.getTotalAmount());
//    }
//
//    @Test
//    void testCreateOrderPlacedEvent_WithEmptyOrderItems() {
//        // Arrange
//        testOrder.setOrderItems(Arrays.asList());
//
//        // Act
//        orderEventPublisher.publishOrderPlacedEvent(testOrder);
//
//        // Assert
//        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
//        verify(rabbitTemplate, times(1)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                eq(RabbitMQConfig.ORDER_ROUTING_KEY_PLACED),
//                eventCaptor.capture()
//        );
//
//        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
//        assertNotNull(capturedEvent.getOrderItems());
//        assertTrue(capturedEvent.getOrderItems().isEmpty());
//    }
//
//    @Test
//    void testMultipleEventPublications() {
//        // Act
//        orderEventPublisher.publishOrderPlacedEvent(testOrder);
//        orderEventPublisher.publishOrderStatusUpdatedEvent(testOrder);
//        orderEventPublisher.publishOrderCancelledEvent(testOrder);
//        orderEventPublisher.publishOrderRatedEvent(testOrder);
//
//        // Assert
//        verify(rabbitTemplate, times(4)).convertAndSend(
//                eq(RabbitMQConfig.ORDER_EXCHANGE),
//                any(String.class),
//                any(OrderPlacedEvent.class)
//        );
//    }
//
//    private void assertOrderPlacedEvent(OrderPlacedEvent event, String expectedEventType) {
//        // Assert basic order properties
//        assertEquals(testOrder.getId(), event.getOrderId());
//        assertEquals(testOrder.getCustomerId(), event.getCustomerId());
//        assertEquals(testOrder.getRestaurantId(), event.getRestaurantId());
//        assertEquals(testOrder.getRestaurantName(), event.getRestaurantName());
//        assertEquals(testOrder.getTotalAmount(), event.getTotalAmount());
//        assertEquals(testOrder.getCreatedAt(), event.getOrderTime());
//        assertEquals(testOrder.getCustomerEmail(), event.getCustomerEmail());
//        assertEquals(testOrder.getDeliveryAddress(), event.getDeliveryAddress());
//        assertEquals(testOrder.getDeliveryPhone(), event.getDeliveryPhone());
//        assertEquals(testOrder.getStatus().name(), event.getStatus());
//        assertEquals(expectedEventType, event.getEventType());
//
//        // Assert order items
//        assertNotNull(event.getOrderItems());
//        assertEquals(2, event.getOrderItems().size());
//
//        OrderPlacedEvent.OrderItemEvent item1 = event.getOrderItems().get(0);
//        assertEquals(orderItem1.getMenuItemId(), item1.getMenuItemId());
//        assertEquals(orderItem1.getMenuItemName(), item1.getMenuItemName());
//        assertEquals(orderItem1.getQuantity(), item1.getQuantity());
//        assertEquals(orderItem1.getUnitPrice(), item1.getUnitPrice());
//
//        OrderPlacedEvent.OrderItemEvent item2 = event.getOrderItems().get(1);
//        assertEquals(orderItem2.getMenuItemId(), item2.getMenuItemId());
//        assertEquals(orderItem2.getMenuItemName(), item2.getMenuItemName());
//        assertEquals(orderItem2.getQuantity(), item2.getQuantity());
//        assertEquals(orderItem2.getUnitPrice(), item2.getUnitPrice());
//    }
//}