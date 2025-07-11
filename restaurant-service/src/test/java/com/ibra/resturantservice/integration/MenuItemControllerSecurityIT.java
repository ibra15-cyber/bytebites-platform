package com.ibra.resturantservice.integration;

import com.ibra.enums.MenuItemCategory;
import com.ibra.resturantservice.dto.CreateMenuItemRequest;
import com.ibra.resturantservice.dto.CreateRestaurantRequest;
import com.ibra.resturantservice.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MenuItemControllerSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantService restaurantService;

    private Long restaurantId;
    private final Long ownerId = 2L;

    @BeforeEach
    void setUp() {
        // Create a test restaurant owned by user 2
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        CreateRestaurantRequest restaurantRequest = new CreateRestaurantRequest(
                "Test Restaurant",
                "Description",
                "123 Main St",
                "+12345" + uniqueId,  // Unique phone
                "owner+" + uniqueId + "@test.com",  // Unique email
                "http://image.url"
        );
        restaurantId = restaurantService.createRestaurant(restaurantRequest, ownerId).getId();
    }

    @Test
    void createMenuItem_WithOwnerRole_Succeeds() throws Exception {
        // Arrange
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger",
                "Delicious",
                BigDecimal.valueOf(9.99),
                MenuItemCategory.MAIN_COURSE,
                "http://image.url"
        );

        // Act & Assert
        mockMvc.perform(post("/api/menu-items/restaurants/" + restaurantId)
                        .header("X-User-Id", ownerId.toString())
                        .header("X-User-Role", "RESTAURANT_OWNER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createMenuItem_WithoutAuthHeaders_BlocksRequest() throws Exception {
        // Arrange
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                "Burger 2",
                "Delicious",
                BigDecimal.valueOf(9.99),
                MenuItemCategory.MAIN_COURSE,
                "http://image.url"
        );

        // Act & Assert
        mockMvc.perform(post("/api/menu-items/restaurants/" + restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}