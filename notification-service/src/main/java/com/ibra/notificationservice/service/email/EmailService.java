package com.ibra.notificationservice.service.email;

import com.ibra.notificationservice.dto.OrderPlacedEvent;
import com.ibra.notificationservice.dto.RestaurantDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.company-name:ByteBites}")
    private String companyName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public CompletableFuture<Void> sendCustomerOrderConfirmation(OrderPlacedEvent event) {
        try {
            String subject = getCustomerSubject(event);
            String content = generateCustomerEmailContent(event);

            sendEmail(event.getCustomerEmail(), subject, content);
            logger.info("Customer order confirmation email sent for order: {}", event.getOrderId());

        } catch (Exception e) {
            logger.error("Failed to send customer email for order: {}", event.getOrderId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendRestaurantOrderNotification(OrderPlacedEvent event, RestaurantDTO restaurant) { // Use shared RestaurantDTO
        try {
            String subject = getRestaurantSubject(event);
            String content = generateRestaurantEmailContent(event, restaurant);

            sendEmail(restaurant.getOwnerEmail(), subject, content); // Use restaurant.getEmail()
            logger.info("Restaurant order notification email sent for order: {} to restaurant: {}",
                    event.getOrderId(), restaurant.getName());

        } catch (Exception e) {
            logger.error("Failed to send restaurant email for order: {} to restaurant: {}",
                    event.getOrderId(), restaurant.getName(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            logger.debug("Email sent successfully to: {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e); // Re-throw to propagate
        }
    }

    private String getCustomerSubject(OrderPlacedEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> "Order Confirmation - #" + event.getOrderId();
            case "ORDER_CANCELLED" -> "Order Cancelled - #" + event.getOrderId();
            case "ORDER_STATUS_UPDATED" -> "Order Status Update - #" + event.getOrderId();
            case "ORDER_RATED" -> "Thank you for rating - #" + event.getOrderId();
            default -> "Order Notification - #" + event.getOrderId();
        };
    }

    private String getRestaurantSubject(OrderPlacedEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_PLACED" -> "New Order Received - #" + event.getOrderId();
            case "ORDER_CANCELLED" -> "Order Cancelled - #" + event.getOrderId();
            case "ORDER_STATUS_UPDATED" -> "Order Status Update - #" + event.getOrderId();
            case "ORDER_RATED" -> "Order Rated - #" + event.getOrderId();
            default -> "Order Notification - #" + event.getOrderId();
        };
    }

    private String generateCustomerEmailContent(OrderPlacedEvent event) {
        StringBuilder content = new StringBuilder();

        content.append("Dear Valued Customer,\n\n");

        switch (event.getEventType()) {
            case "ORDER_PLACED" -> content.append("Thank you for your order! We're excited to prepare your delicious meal.\n\n");
            case "ORDER_CANCELLED" -> content.append("Your order has been cancelled. If you have any questions, please contact us.\n\n");
            case "ORDER_STATUS_UPDATED" -> content.append("Your order status has been updated. Please check your order details below.\n\n");
            case "ORDER_RATED" -> content.append("Thank you for taking the time to rate your order. Your feedback helps us improve!\n\n");
            default -> content.append("Here's an update about your order:\n\n");
        }

        content.append("ORDER DETAILS:\n");
        content.append("Order ID: #").append(event.getOrderId()).append("\n");
        // Use restaurant name from event if available, otherwise just ID
        content.append("Restaurant: ").append(event.getRestaurantName() != null ? event.getRestaurantName() : event.getRestaurantId()).append("\n");
        content.append("Order Date: ").append(event.getOrderTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))).append("\n");
        content.append("Delivery Address: ").append(event.getDeliveryAddress()).append("\n");
        content.append("Delivery Phone: ").append(event.getDeliveryPhone()).append("\n\n");

        content.append("ORDER ITEMS:\n");
        content.append("----------------------------------------\n");

        if (event.getItems() != null) {
            for (OrderPlacedEvent.OrderItemEvent item : event.getItems()) {
                content.append(item.getQuantity()).append("x ")
                        .append(item.getMenuItemName())
                        .append(" - $").append(String.format("%.2f", item.getPriceAtOrderTime()))
                        .append(" each\n");
            }
        }

        content.append("----------------------------------------\n");
        content.append("TOTAL: $").append(String.format("%.2f", event.getTotalAmount())).append("\n\n");

        content.append("Thank you for choosing ").append(companyName).append("!\n\n");
        content.append("Best regards,\n");
        content.append("The ").append(companyName).append(" Team");

        return content.toString();
    }

    private String generateRestaurantEmailContent(OrderPlacedEvent event, RestaurantDTO restaurant) { // Use shared RestaurantDTO
        StringBuilder content = new StringBuilder();

        content.append("Dear ").append(restaurant.getOwnerName() != null ? restaurant.getOwnerName() : "Restaurant Owner").append(",\n\n");

        switch (event.getEventType()) {
            case "ORDER_PLACED" -> content.append("You have received a new order! Please prepare the following items:\n\n");
            case "ORDER_CANCELLED" -> content.append("An order has been cancelled. Please see details below:\n\n");
            case "ORDER_STATUS_UPDATED" -> content.append("An order status has been updated. Please see details below:\n\n");
            case "ORDER_RATED" -> content.append("One of your orders has been rated by the customer. Please see details below:\n\n");
            default -> content.append("Order notification for your restaurant:\n\n");
        }

        content.append("ORDER DETAILS:\n");
        content.append("Order ID: #").append(event.getOrderId()).append("\n");
        content.append("Restaurant: ").append(restaurant.getName() != null ? restaurant.getName() : event.getRestaurantId()).append("\n"); // Use restaurant.getName()
        content.append("Order Date: ").append(event.getOrderTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))).append("\n");
        content.append("Customer Email: ").append(event.getCustomerEmail()).append("\n");
        content.append("Delivery Address: ").append(event.getDeliveryAddress()).append("\n");
        content.append("Delivery Phone: ").append(event.getDeliveryPhone()).append("\n\n");

        content.append("ITEMS TO PREPARE:\n");
        content.append("----------------------------------------\n");

        if (event.getItems() != null) {
            for (OrderPlacedEvent.OrderItemEvent item : event.getItems()) {
                content.append(item.getQuantity()).append("x ")
                        .append(item.getMenuItemName()) // Use menuItemName from event
                        .append(" - $").append(String.format("%.2f", item.getPriceAtOrderTime())) // Use unitPrice
                        .append(" each\n");
            }
        }

        content.append("----------------------------------------\n");
        content.append("ORDER TOTAL: $").append(String.format("%.2f", event.getTotalAmount())).append("\n\n");

        if ("ORDER_PLACED".equals(event.getEventType())) {
            content.append("Please start preparing this order as soon as possible.\n");
        }

        content.append("\nBest regards,\n");
        content.append("The ").append(companyName).append(" Team");

        return content.toString();
    }
}
    