package com.ibra.dto;


import com.ibra.enums.RestaurantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDTO {

    // Getters and Setters
    private Long id;

    @NotBlank(message = "Restaurant name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Email(message = "Valid email is required")
    private String email;

    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private String imageUrl;
    private RestaurantStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MenuItemDTO> menuItems;
    private String cuisine;
    private Double rate;
}
