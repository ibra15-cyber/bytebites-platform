package com.ibra.resturantservice.mapper;

import com.ibra.resturantservice.dto.MenuItemDTO;
import com.ibra.resturantservice.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItemDTO toDTO(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }

        MenuItemDTO dto = new MenuItemDTO();
        dto.setId(menuItem.getId());
        dto.setName(menuItem.getName());
        dto.setDescription(menuItem.getDescription());
        dto.setPrice(menuItem.getPrice());
        dto.setRestaurantId(menuItem.getRestaurant().getId());
        dto.setCategory(menuItem.getCategory());
        dto.setImageUrl(menuItem.getImageUrl());
        dto.setStatus(menuItem.getStatus());
        dto.setCreatedAt(menuItem.getCreatedAt());
        dto.setUpdatedAt(menuItem.getUpdatedAt());

        // Set restaurant ID to avoid circular reference
        if (menuItem.getRestaurant() != null) {
            dto.setRestaurantId(menuItem.getRestaurant().getId());
        }

        return dto;
    }

    public MenuItem toEntity(MenuItemDTO dto) {
        if (dto == null) {
            return null;
        }

        MenuItem menuItem = new MenuItem();
        menuItem.setId(dto.getId());
        menuItem.setName(dto.getName());
        menuItem.setDescription(dto.getDescription());
        menuItem.setPrice(dto.getPrice());
        menuItem.setCategory(dto.getCategory());
        menuItem.setImageUrl(dto.getImageUrl());
        menuItem.setStatus(dto.getStatus());
        menuItem.setCreatedAt(dto.getCreatedAt());
        menuItem.setUpdatedAt(dto.getUpdatedAt());

        return menuItem;
    }
}
