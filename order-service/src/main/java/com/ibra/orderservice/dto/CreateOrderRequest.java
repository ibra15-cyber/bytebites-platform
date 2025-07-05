package com.ibra.orderservice.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CreateOrderRequest {

    // Getters and Setters
    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    @NotBlank(message = "Delivery phone is required")
    private String deliveryPhone;

    private String specialInstructions;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;

    // Constructors
    public CreateOrderRequest() {}

}

