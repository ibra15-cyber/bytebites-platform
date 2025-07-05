package com.ibra.orderservice.mapper;


import com.ibra.orderservice.dto.OrderDTO;
import com.ibra.orderservice.dto.OrderItemDTO;
import com.ibra.orderservice.entity.Order;
import com.ibra.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order order) {
        if (order == null) {
            return null;
        }

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomerId());
        dto.setRestaurantId(order.getRestaurantId());
        dto.setRestaurantName(order.getRestaurantName());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setDeliveryPhone(order.getDeliveryPhone());
        dto.setSpecialInstructions(order.getSpecialInstructions());
        dto.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setRating(order.getRating());
        dto.setReview(order.getReview());

        if (order.getOrderItems() != null) {
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::toOrderItemDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public OrderItemDTO toOrderItemDTO(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setMenuItemId(orderItem.getMenuItemId());
        dto.setItemName(orderItem.getMenuItemName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setUnitPrice(orderItem.getUnitPrice());
        dto.setTotalPrice(orderItem.getUnitPrice().multiply(java.math.BigDecimal.valueOf(orderItem.getQuantity())));
        dto.setSpecialInstructions(orderItem.getSpecialInstructions());

        return dto;
    }

    public List<OrderDTO> toDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }

        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}