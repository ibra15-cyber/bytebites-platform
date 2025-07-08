//package com.ibra.notificationservice.service.rabbitmq;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.ibra.dto.OrderPlacedEvent;
//import com.ibra.notificationservice.service.NotificationService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class OrderEventListenerTest {
//
//    @Mock
//    private NotificationService notificationService;
//
//    @InjectMocks
//    private OrderEventListener orderEventListener;
//
//    private OrderPlacedEvent orderPlacedEvent;
//
//    @BeforeEach
//    void setUp() {
//        // Set up the queue name property (simulating @Value injection)
//        ReflectionTestUtils.setField(orderEventListener, "notificationQueueName", "notification.order.queue");
//
//        // Create a sample OrderPlacedEvent
//        orderPlacedEvent = new OrderPlacedEvent();
//        orderPlacedEvent.setOrderId(123L);
//        orderPlacedEvent.setEventType("ORDER_PLACED");
//        orderPlacedEvent.setCustomerId(456L);
//        orderPlacedEvent.setTotalAmount(99.99);
//        orderPlacedEvent.setCustomerEmail("example@email.com");
//    }
//
//    @Test
//    void testHandleOrderEvent_Success() {
//        // Given
//        doNothing().when(notificationService).processOrderEvent(orderPlacedEvent);
//
//        // When
//        assertDoesNotThrow(() -> orderEventListener.handleOrderEvent(orderPlacedEvent));
//
//        // Then
//        verify(notificationService, times(1)).processOrderEvent(orderPlacedEvent);
//    }
//
//    @Test
//    void testHandleOrderEvent_ServiceThrowsException() {
//        // Given
//        RuntimeException serviceException = new RuntimeException("Service processing failed");
//        doThrow(serviceException).when(notificationService).processOrderEvent(orderPlacedEvent);
//
//        // When & Then
//        RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
//                orderEventListener.handleOrderEvent(orderPlacedEvent)
//        );
//
//        assertEquals("Service processing failed", thrownException.getMessage());
//        verify(notificationService, times(1)).processOrderEvent(orderPlacedEvent);
//    }
//
//    @Test
//    void testHandleOrderEvent_NullEvent() {
//        // Given
//        OrderPlacedEvent nullEvent = null;
//
//        // When & Then
//        assertThrows(Exception.class, () -> orderEventListener.handleOrderEvent(nullEvent));
//
//        // Verify service was not called
//        verify(notificationService, never()).processOrderEvent(any());
//    }
//
//    @Test
//    void testHandleOrderEvent_EventWithNullOrderId() {
//        // Given
//        OrderPlacedEvent eventWithNullOrderId = new OrderPlacedEvent();
//        eventWithNullOrderId.setOrderId(null);
//        eventWithNullOrderId.setEventType("ORDER_PLACED");
//        eventWithNullOrderId.setCustomerId(456L);
//
//        doNothing().when(notificationService).processOrderEvent(eventWithNullOrderId);
//
//        // When
//        assertDoesNotThrow(() -> orderEventListener.handleOrderEvent(eventWithNullOrderId));
//
//        // Then
//        verify(notificationService, times(1)).processOrderEvent(eventWithNullOrderId);
//    }
//
//    @Test
//    void testHandleOrderEvent_MultipleCalls() {
//        // Given
//        OrderPlacedEvent event1 = new OrderPlacedEvent();
//        event1.setOrderId(1L);
//        event1.setEventType("ORDER_PLACED");
//
//        OrderPlacedEvent event2 = new OrderPlacedEvent();
//        event2.setOrderId(2L);
//        event2.setEventType("ORDER_CANCELLED");
//
//        doNothing().when(notificationService).processOrderEvent(any(OrderPlacedEvent.class));
//
//        // When
//        assertDoesNotThrow(() -> {
//            orderEventListener.handleOrderEvent(event1);
//            orderEventListener.handleOrderEvent(event2);
//        });
//
//        // Then
//        verify(notificationService, times(2)).processOrderEvent(any(OrderPlacedEvent.class));
//        verify(notificationService, times(1)).processOrderEvent(event1);
//        verify(notificationService, times(1)).processOrderEvent(event2);
//    }
//
//    @Test
//    void testConstructor() {
//        // Given
//        NotificationService mockService = mock(NotificationService.class);
//
//        // When
//        OrderEventListener listener = new OrderEventListener(mockService);
//
//        // Then
//        assertNotNull(listener);
//        // Verify that the service is properly injected (we can't directly access it, but we can test behavior)
//        OrderPlacedEvent testEvent = new OrderPlacedEvent();
//        testEvent.setOrderId(1L);
//        testEvent.setEventType("ORDER_PLACED");
//
//        doNothing().when(mockService).processOrderEvent(testEvent);
//        assertDoesNotThrow(() -> listener.handleOrderEvent(testEvent));
//        verify(mockService, times(1)).processOrderEvent(testEvent);
//    }
//}