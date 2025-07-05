package com.ibra.orderservice.dto;

import lombok.Data;

@Data
public class RestaurantDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private String cuisine;
    private Boolean isActive;
    private Double rating;
    private String imageUrl;
}