package com.ibra.notificationservice.service.email;

import static org.junit.jupiter.api.Assertions.*;

import com.ibra.notificationservice.dto.OrderPlacedEvent;
import com.ibra.notificationservice.dto.RestaurantDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private OrderPlacedEvent orderPlacedEvent;
    private RestaurantDTO restaurantDTO;

    @BeforeEach
    void setUp() {
        // Set up @Value annotated fields
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@bytebites.com");
        ReflectionTestUtils.setField(emailService, "companyName", "ByteBites");

        // Create test OrderPlacedEvent
        orderPlacedEvent = new OrderPlacedEvent();
        orderPlacedEvent.setOrderId(123L);
        orderPlacedEvent.setEventType("ORDER_PLACED");
        orderPlacedEvent.setCustomerId(456L);
        orderPlacedEvent.setCustomerEmail("customer@example.com");
        orderPlacedEvent.setRestaurantId(789L);
        orderPlacedEvent.setRestaurantName("Test Restaurant");
        orderPlacedEvent.setOrderTime(LocalDateTime.of(2023, 12, 15, 14, 30));
        orderPlacedEvent.setDeliveryAddress("123 Main St, City, State 12345");
        orderPlacedEvent.setDeliveryPhone("(555) 123-4567");
        orderPlacedEvent.setTotalAmount(29.99);

        // Create test order items
        OrderPlacedEvent.OrderItemEvent item1 = new OrderPlacedEvent.OrderItemEvent();
        item1.setMenuItemName("Burger");
        item1.setQuantity(2);
        item1.setPriceAtOrderTime(12.99);

        OrderPlacedEvent.OrderItemEvent item2 = new OrderPlacedEvent.OrderItemEvent();
        item2.setMenuItemName("Fries");
        item2.setQuantity(1);
        item2.setPriceAtOrderTime(4.99);

        orderPlacedEvent.setItems(Arrays.asList(item1, item2));

        // Create test RestaurantDTO
        restaurantDTO = new RestaurantDTO();
        restaurantDTO.setId(789L);
        restaurantDTO.setName("Test Restaurant");
        restaurantDTO.setOwnerName("John Doe");
        restaurantDTO.setOwnerEmail("owner@testrestaurant.com");
    }

    @Test
    void testSendCustomerOrderConfirmation_Success() throws ExecutionException, InterruptedException {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);

        // Then
        assertNotNull(result);
        result.get(); // Wait for completion

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("test@bytebites.com", sentMessage.getFrom());
        assertEquals("customer@example.com", sentMessage.getTo()[0]);
        assertEquals("Order Confirmation - #123", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("Dear Valued Customer"));
        assertTrue(sentMessage.getText().contains("#123"));
        assertTrue(sentMessage.getText().contains("Test Restaurant"));
        assertTrue(sentMessage.getText().contains("2x Burger - $12.99 each"));
        assertTrue(sentMessage.getText().contains("TOTAL: $29.99"));
    }

    @Test
    void testSendRestaurantOrderNotification_Success() throws ExecutionException, InterruptedException {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendRestaurantOrderNotification(orderPlacedEvent, restaurantDTO);

        // Then
        assertNotNull(result);
        result.get(); // Wait for completion

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("test@bytebites.com", sentMessage.getFrom());
        assertEquals("owner@testrestaurant.com", sentMessage.getTo()[0]);
        assertEquals("New Order Received - #123", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("Dear John Doe"));
        assertTrue(sentMessage.getText().contains("You have received a new order"));
        assertTrue(sentMessage.getText().contains("#123"));
        assertTrue(sentMessage.getText().contains("Test Restaurant"));
        assertTrue(sentMessage.getText().contains("2x Burger - $12.99 each"));
    }

    @Test
    void testSendEmail_MailSenderThrowsException() {
        // Given
        doThrow(new RuntimeException("SMTP server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertDoesNotThrow(() -> {
            CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
            try {
                result.get();
            } catch (Exception e) {
                // Expected in async context
            }
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testCustomerEmailSubjects_DifferentEventTypes() throws ExecutionException, InterruptedException {
        // Test ORDER_CANCELLED
        orderPlacedEvent.setEventType("ORDER_CANCELLED");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals("Order Cancelled - #123", messageCaptor.getValue().getSubject());

        // Test ORDER_STATUS_UPDATED
        reset(mailSender);
        orderPlacedEvent.setEventType("ORDER_STATUS_UPDATED");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals("Order Status Update - #123", messageCaptor.getValue().getSubject());

        // Test ORDER_RATED
        reset(mailSender);
        orderPlacedEvent.setEventType("ORDER_RATED");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals("Thank you for rating - #123", messageCaptor.getValue().getSubject());
    }

    @Test
    void testRestaurantEmailSubjects_DifferentEventTypes() throws ExecutionException, InterruptedException {
        // Test ORDER_CANCELLED
        orderPlacedEvent.setEventType("ORDER_CANCELLED");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        CompletableFuture<Void> result = emailService.sendRestaurantOrderNotification(orderPlacedEvent, restaurantDTO);
        result.get();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals("Order Cancelled - #123", messageCaptor.getValue().getSubject());

        // Test unknown event type
        reset(mailSender);
        orderPlacedEvent.setEventType("UNKNOWN_EVENT");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        result = emailService.sendRestaurantOrderNotification(orderPlacedEvent, restaurantDTO);
        result.get();

        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertEquals("Order Notification - #123", messageCaptor.getValue().getSubject());
    }

    @Test
    void testCustomerEmailContent_NullRestaurantName() throws ExecutionException, InterruptedException {
        // Given
        orderPlacedEvent.setRestaurantName(null);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Restaurant: 789"));
    }

    @Test
    void testRestaurantEmailContent_NullOwnerName() throws ExecutionException, InterruptedException {
        // Given
        restaurantDTO.setOwnerName(null);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendRestaurantOrderNotification(orderPlacedEvent, restaurantDTO);
        result.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Dear Restaurant Owner"));
    }

    @Test
    void testEmailContent_NullItems() throws ExecutionException, InterruptedException {
        // Given
        orderPlacedEvent.setItems(null);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("ORDER ITEMS:"));
        assertTrue(sentMessage.getText().contains("TOTAL: $29.99"));
        // Should not contain any item details
        assertFalse(sentMessage.getText().contains("Burger"));
    }

    @Test
    void testEmailContent_EmptyItems() throws ExecutionException, InterruptedException {
        // Given
        orderPlacedEvent.setItems(Arrays.asList());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("ORDER ITEMS:"));
        assertTrue(sentMessage.getText().contains("TOTAL: $29.99"));
        // Should not contain any item details
        assertFalse(sentMessage.getText().contains("Burger"));
    }

    @Test
    void testEmailAddresses_ConfiguredCorrectly() throws ExecutionException, InterruptedException {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When - Send both types of emails
        CompletableFuture<Void> customerResult = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        CompletableFuture<Void> restaurantResult = emailService.sendRestaurantOrderNotification(orderPlacedEvent, restaurantDTO);

        customerResult.get();
        restaurantResult.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());

        var sentMessages = messageCaptor.getAllValues();

        // Customer email
        assertEquals("test@bytebites.com", sentMessages.get(0).getFrom());
        assertEquals("customer@example.com", sentMessages.get(0).getTo()[0]);

        // Restaurant email
        assertEquals("test@bytebites.com", sentMessages.get(1).getFrom());
        assertEquals("owner@testrestaurant.com", sentMessages.get(1).getTo()[0]);
    }

    @Test
    void testEmailContent_FormattingAndCompanyName() throws ExecutionException, InterruptedException {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Void> result = emailService.sendCustomerOrderConfirmation(orderPlacedEvent);
        result.get();

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String emailContent = sentMessage.getText();

        // Check date formatting
        assertTrue(emailContent.contains("Dec 15, 2023 at 14:30"));

        // Check company name usage
        assertTrue(emailContent.contains("Thank you for choosing ByteBites!"));
        assertTrue(emailContent.contains("The ByteBites Team"));

        // Check price formatting
        assertTrue(emailContent.contains("$12.99 each"));
        assertTrue(emailContent.contains("TOTAL: $29.99"));
    }
}