package com.ibra.notificationservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ibra.dto.OrderPlacedEvent;
import com.ibra.dto.RestaurantDTO;
import com.ibra.enums.RestaurantStatus;
import com.ibra.notificationservice.service.email.EmailService;
import com.ibra.notificationservice.service.external.RestaurantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private RestaurantClient restaurantClient;

    @InjectMocks
    private NotificationService notificationService;

    private OrderPlacedEvent testOrderEvent;
    private RestaurantDTO testRestaurant;
    private List<OrderPlacedEvent.OrderItemEvent> testItems;

    @BeforeEach
    void setUp() {
        // Create test order items
        OrderPlacedEvent.OrderItemEvent item1 = new OrderPlacedEvent.OrderItemEvent(
                1L, "Burger", 2, BigDecimal.valueOf(15.99)
        );
        OrderPlacedEvent.OrderItemEvent item2 = new OrderPlacedEvent.OrderItemEvent(
                2L, "Fries", 1, BigDecimal.valueOf(5.99)
        );
        testItems = Arrays.asList(item1, item2);

        // Create test order event
        testOrderEvent = new OrderPlacedEvent(
                123L,                           // orderId
                456L,                           // customerId
                789L,                           // restaurantId
                "Test Restaurant",              // restaurantName
                "123 Test Street",              // deliveryAddress
                "123-456-7890",                 // deliveryPhone
                "customer@example.com",         // customerEmail
                testItems,                      // items
                LocalDateTime.now(),            // orderTime
                "PENDING",                      // status
                BigDecimal.valueOf(37.97),                          // totalAmount
                "ORDER_PLACED"                  // eventType
        );

        // Create test restaurant
        testRestaurant = new RestaurantDTO(
//                789L,                           // id
//                "Test Restaurant",              // name
//                "owner@restaurant.com",         // ownerEmail
//                "John Doe",                     // ownerName
//                "987-654-3210",                 // phone
//                "456 Restaurant Ave",           // address
//                "Great food place",             // description
//                "Italian",                      // cuisine
//                RestaurantStatus.ACTIVE         // isActive
        );
    }

    @Test
    void testProcessOrderEvent_Success() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_CustomerNotificationAlwaysSent() {
        // Arrange - Restaurant client will throw exception
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenThrow(new RuntimeException("Restaurant service unavailable"));

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert - Customer notification should still be sent
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }

    @Test
    void testProcessOrderEvent_RestaurantNotFound() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(null);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }

    @Test
    void testProcessOrderEvent_RestaurantWithNullId() {
        // Arrange
        RestaurantDTO restaurantWithNullId = new RestaurantDTO();
        restaurantWithNullId.setId(null);
        restaurantWithNullId.setName("Test Restaurant");

        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(restaurantWithNullId);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }

    @Test
    void testProcessOrderEvent_RestaurantClientThrowsException() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> notificationService.processOrderEvent(testOrderEvent));

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }



    @Test
    void testProcessOrderEvent_RestaurantEmailServiceThrowsException() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);
        doThrow(new RuntimeException("Restaurant email service error"))
                .when(emailService).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> notificationService.processOrderEvent(testOrderEvent));

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_WithDifferentEventTypes() {
        // Test ORDER_CANCELLED event
        testOrderEvent.setEventType("ORDER_CANCELLED");
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        notificationService.processOrderEvent(testOrderEvent);

        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);

        // Reset mocks for next test
        reset(emailService, restaurantClient);

        // Test ORDER_STATUS_UPDATED event
        testOrderEvent.setEventType("ORDER_STATUS_UPDATED");
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        notificationService.processOrderEvent(testOrderEvent);

        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }


    @Test
    void testProcessOrderEvent_WithNullRestaurantId() {
        // Arrange
        testOrderEvent.setRestaurantId(null);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(null);
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }

    @Test
    void testProcessOrderEvent_WithEmptyOrderItems() {
        // Arrange
        testOrderEvent.setOrderItems(Collections.emptyList());
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_WithNullOrderItems() {
        // Arrange
        testOrderEvent.setOrderItems(null);
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_ServiceCallOrder() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert - Verify the order of service calls
        InOrder inOrder = inOrder(emailService, restaurantClient);
        inOrder.verify(emailService).sendCustomerOrderConfirmation(testOrderEvent);
        inOrder.verify(restaurantClient).getRestaurantById(testOrderEvent.getRestaurantId());
        inOrder.verify(emailService).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_MultipleCallsWithSameEvent() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);
        notificationService.processOrderEvent(testOrderEvent);
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(3)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(3)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(3)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_WithInactiveRestaurant() {
        // Arrange
        testRestaurant.setStatus(RestaurantStatus.INACTIVE);
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert - Should still process the notification even if restaurant is inactive
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_WithSpecialCharactersInData() {
        // Arrange
        testOrderEvent.setRestaurantName("Test Restaurant & Café");
        testOrderEvent.setCustomerEmail("test+user@example.com");
        testOrderEvent.setDeliveryAddress("123 Test Street, Apt #5");

        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenReturn(testRestaurant);

        // Act
        notificationService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, times(1)).sendRestaurantOrderNotification(testOrderEvent, testRestaurant);
    }

    @Test
    void testProcessOrderEvent_VerifyExceptionHandling() {
        // Arrange
        when(restaurantClient.getRestaurantById(testOrderEvent.getRestaurantId()))
                .thenThrow(new RuntimeException("Database connection lost"));

        // Act - Should not propagate exception
        assertDoesNotThrow(() -> notificationService.processOrderEvent(testOrderEvent));

        // Assert
        verify(emailService, times(1)).sendCustomerOrderConfirmation(testOrderEvent);
        verify(restaurantClient, times(1)).getRestaurantById(testOrderEvent.getRestaurantId());
        verify(emailService, never()).sendRestaurantOrderNotification(any(), any());
    }
}