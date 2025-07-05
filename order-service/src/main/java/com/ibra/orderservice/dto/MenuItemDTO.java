package com.ibra.orderservice.dto;


import com.ibra.orderservice.enums.MenuItemCategory;
import com.ibra.orderservice.enums.MenuItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class MenuItemDTO {

    // Getters and Setters
    private Long id;

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private Long restaurantId;

    @NotNull(message = "Category is required")
    private MenuItemCategory category;

    private String imageUrl;
    private MenuItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public MenuItemDTO() {}

}
