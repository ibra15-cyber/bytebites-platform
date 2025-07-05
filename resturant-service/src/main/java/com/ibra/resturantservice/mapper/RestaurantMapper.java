package com.ibra.resturantservice.mapper;


import com.ibra.resturantservice.dto.RestaurantDTO;
import com.ibra.resturantservice.entity.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RestaurantMapper {

    @Autowired
    private MenuItemMapper menuItemMapper;

    public RestaurantDTO toDTO(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }

        RestaurantDTO dto = new RestaurantDTO();
        dto.setId(restaurant.getId());
        dto.setName(restaurant.getName());
        dto.setDescription(restaurant.getDescription());
        dto.setAddress(restaurant.getAddress());
        dto.setPhoneNumber(restaurant.getPhoneNumber());
        dto.setEmail(restaurant.getEmail());
        dto.setOwnerId(restaurant.getOwnerId());
        dto.setImageUrl(restaurant.getImageUrl());
        dto.setStatus(restaurant.getStatus());
        dto.setCreatedAt(restaurant.getCreatedAt());
        dto.setUpdatedAt(restaurant.getUpdatedAt());

        // Map menu items if they are loaded (avoid lazy loading issues)
        if (restaurant.getMenuItems() != null) {
            dto.setMenuItems(restaurant.getMenuItems().stream()
                    .map(menuItemMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Restaurant toEntity(RestaurantDTO dto) {
        if (dto == null) {
            return null;
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setId(dto.getId());
        restaurant.setName(dto.getName());
        restaurant.setDescription(dto.getDescription());
        restaurant.setAddress(dto.getAddress());
        restaurant.setPhoneNumber(dto.getPhoneNumber());
        restaurant.setEmail(dto.getEmail());
        restaurant.setOwnerId(dto.getOwnerId());
        restaurant.setImageUrl(dto.getImageUrl());
        restaurant.setStatus(dto.getStatus());
        restaurant.setCreatedAt(dto.getCreatedAt());
        restaurant.setUpdatedAt(dto.getUpdatedAt());

        return restaurant;
    }
}