package com.ibra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private String restaurantName;
    private String deliveryAddress;
    private String deliveryPhone;
    private String customerEmail;
    private List<OrderItemEvent> orderItems;
    private LocalDateTime orderTime;
    private String status;
    private BigDecimal totalAmount;
    private String eventType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long menuItemId;
        private String menuItemName;
        private int quantity;
        private BigDecimal unitPrice;
    }
}