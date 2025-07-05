package com.ibra.notificationservice.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RestaurantDTO {
    private Long id;
    private String name;
    private String ownerEmail;
    private String ownerName;
    private String phone;
    private String address;
    private String description;
    private String cuisine;
    private boolean isActive;
}
