package com.ibra.orderservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ibra.orderservice.dto.*;
import com.ibra.orderservice.entity.Order;
import com.ibra.orderservice.entity.OrderItem;
import com.ibra.orderservice.enums.MenuItemCategory;
import com.ibra.orderservice.enums.MenuItemStatus;
import com.ibra.orderservice.enums.OrderStatus;
import com.ibra.orderservice.exception.BusinessException;
import com.ibra.orderservice.exception.ResourceNotFoundException;
import com.ibra.orderservice.exception.UnauthorizedException;
import com.ibra.orderservice.mapper.OrderMapper;
import com.ibra.orderservice.repository.OrderItemRepository;
import com.ibra.orderservice.repository.OrderRepository;
import com.ibra.orderservice.service.external.RestaurantServiceClient;
import com.ibra.orderservice.service.rabbitmq.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private RestaurantDTO restaurantDTO;
    private MenuItemDTO menuItemDTO;
    private Order order;
    private OrderDTO orderDTO;
    private OrderItem orderItem;
    private OrderItemDTO orderItemDTO;
    private ApiResponse<RestaurantDTO> restaurantResponse;
    private ApiResponse<MenuItemDTO> menuItemResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        setupCreateOrderRequest();
        setupRestaurantDTO();
        setupMenuItemDTO();
        setupOrder();
        setupOrderDTO();
        setupOrderItem();
        setupOrderItemDTO();
        setupApiResponses();
    }

    private void setupCreateOrderRequest() {
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setRestaurantId(1L);
        createOrderRequest.setDeliveryAddress("123 Test Street");
        createOrderRequest.setDeliveryPhone("1234567890");
        createOrderRequest.setSpecialInstructions("Test instructions");

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setMenuItemId(1L);
        orderItemRequest.setQuantity(2);
        orderItemRequest.setSpecialInstructions("Extra spicy");

        createOrderRequest.setOrderItems(Arrays.asList(orderItemRequest));
    }

    private void setupRestaurantDTO() {
        restaurantDTO = new RestaurantDTO();
        restaurantDTO.setId(1L);
        restaurantDTO.setName("Test Restaurant");
        restaurantDTO.setDescription("Test Description");
        restaurantDTO.setAddress("Restaurant Address");
        restaurantDTO.setPhone("9876543210");
        restaurantDTO.setEmail("restaurant@test.com");
        restaurantDTO.setCuisine("Italian");
        restaurantDTO.setIsActive(true);
        restaurantDTO.setRating(4.5);
    }

    private void setupMenuItemDTO() {
        menuItemDTO = new MenuItemDTO();
        menuItemDTO.setId(1L);
        menuItemDTO.setName("Test Pizza");
        menuItemDTO.setDescription("Delicious pizza");
        menuItemDTO.setPrice(new BigDecimal("15.99"));
        menuItemDTO.setRestaurantId(1L);
        menuItemDTO.setCategory(MenuItemCategory.MAIN_COURSE);
        menuItemDTO.setStatus(MenuItemStatus.AVAILABLE);
        menuItemDTO.setCreatedAt(LocalDateTime.now());
    }

    private void setupOrder() {
        order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setRestaurantId(1L);
        order.setRestaurantName("Test Restaurant");
        order.setCustomerEmail("customer@test.com");
        order.setTotalAmount(new BigDecimal("31.98"));
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddress("123 Test Street");
        order.setDeliveryPhone("1234567890");
        order.setSpecialInstructions("Test instructions");
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
    }

    private void setupOrderDTO() {
        orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setCustomerId(1L);
        orderDTO.setRestaurantId(1L);
        orderDTO.setRestaurantName("Test Restaurant");
        orderDTO.setCustomerEmail("customer@test.com");
        orderDTO.setTotalAmount(new BigDecimal("31.98"));
        orderDTO.setStatus(OrderStatus.PENDING);
        orderDTO.setDeliveryAddress("123 Test Street");
        orderDTO.setDeliveryPhone("1234567890");
        orderDTO.setSpecialInstructions("Test instructions");
        orderDTO.setOrderTime(LocalDateTime.now());
        orderDTO.setCreatedAt(LocalDateTime.now());
        orderDTO.setUpdatedAt(LocalDateTime.now());
    }

    private void setupOrderItem() {
        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setMenuItemId(1L);
        orderItem.setMenuItemName("Test Pizza");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("15.99"));
        orderItem.setSpecialInstructions("Extra spicy");
    }

    private void setupOrderItemDTO() {
        orderItemDTO = new OrderItemDTO();
        orderItemDTO.setId(1L);
        orderItemDTO.setMenuItemId(1L);
        orderItemDTO.setItemName("Test Pizza");
        orderItemDTO.setQuantity(2);
        orderItemDTO.setUnitPrice(new BigDecimal("15.99"));
        orderItemDTO.setTotalPrice(new BigDecimal("31.98"));
        orderItemDTO.setSpecialInstructions("Extra spicy");
        orderItemDTO.setCreatedAt(LocalDateTime.now());
    }

    private void setupApiResponses() {
        restaurantResponse = ApiResponse.<RestaurantDTO>builder()
                .success(true)
                .message("Success")
                .data(restaurantDTO)
                .build();

        menuItemResponse = ApiResponse.<MenuItemDTO>builder()
                .success(true)
                .message("Success")
                .data(menuItemDTO)
                .build();
    }

    @Test
    void createOrder_RestaurantNotFound() {
        // Arrange
        ApiResponse<RestaurantDTO> failedResponse = ApiResponse.<RestaurantDTO>builder()
                .success(false)
                .message("Restaurant not found")
                .data(null)
                .build();
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(failedResponse);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.createOrder(createOrderRequest, 1L, "customer@test.com"));
    }

    @Test
    void createOrder_InvalidMenuItem() {
        // Arrange
        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(restaurantResponse);
        ApiResponse<MenuItemDTO> failedMenuItemResponse = ApiResponse.<MenuItemDTO>builder()
                .success(false)
                .message("Menu item not found")
                .data(null)
                .build();
        when(restaurantServiceClient.getMenuItemById(1L)).thenReturn(failedMenuItemResponse);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.createOrder(createOrderRequest, 1L, "customer@test.com"));
    }

    @Test
    void createOrder_MenuItemFromDifferentRestaurant() {
        // Arrange
        MenuItemDTO wrongRestaurantMenuItem = new MenuItemDTO();
        wrongRestaurantMenuItem.setId(1L);
        wrongRestaurantMenuItem.setRestaurantId(2L); // Different restaurant
        wrongRestaurantMenuItem.setPrice(new BigDecimal("15.99"));

        ApiResponse<MenuItemDTO> wrongRestaurantResponse = ApiResponse.<MenuItemDTO>builder()
                .success(true)
                .message("Success")
                .data(wrongRestaurantMenuItem)
                .build();

        when(restaurantServiceClient.getRestaurantById(1L)).thenReturn(restaurantResponse);
        when(restaurantServiceClient.getMenuItemById(1L)).thenReturn(wrongRestaurantResponse);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.createOrder(createOrderRequest, 1L, "customer@test.com"));
    }

    @Test
    void getOrdersByCustomer_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order), pageable, 1);
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Act
        Page<OrderDTO> result = orderService.getOrdersByCustomer(1L, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(orderDTO.getId(), result.getContent().get(0).getId());
        verify(orderRepository).findByCustomerIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getOrderById_Success_Customer() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Act
        OrderDTO result = orderService.getOrderById(1L, 1L, "CUSTOMER");

        // Assert
        assertNotNull(result);
        assertEquals(orderDTO.getId(), result.getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderById(1L, 1L, "CUSTOMER"));
    }

    @Test
    void getOrderById_UnauthorizedCustomer() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                orderService.getOrderById(1L, 2L, "CUSTOMER")); // Different customer ID
    }

    @Test
    void cancelOrder_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Act
        OrderDTO result = orderService.cancelOrder(1L, 1L);

        // Assert
        assertNotNull(result);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventPublisher).publishOrderCancelledEvent(any(Order.class));
    }

    @Test
    void cancelOrder_UnauthorizedCustomer() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                orderService.cancelOrder(1L, 2L)); // Different customer ID
    }

    @Test
    void cancelOrder_InvalidStatus() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED); // Cannot cancel delivered order
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.cancelOrder(1L, 1L));
    }

    @Test
    void updateOrderStatus_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Mock restaurant service for restaurant owner verification
        when(restaurantServiceClient.getRestaurantByOwnerId(1L)).thenReturn(restaurantResponse);

        // Act
        OrderDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED, 1L, "RESTAURANT_OWNER");

        // Assert
        assertNotNull(result);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventPublisher).publishOrderStatusUpdatedEvent(any(Order.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED); // Terminal state
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.updateOrderStatus(1L, OrderStatus.PREPARING, 1L, "ADMIN"));
    }

    @Test
    void getOrdersByRestaurant_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order), pageable, 1);
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Mock restaurant service for restaurant owner verification
        when(restaurantServiceClient.getRestaurantByOwnerId(1L)).thenReturn(restaurantResponse);

        // Act
        Page<OrderDTO> result = orderService.getOrdersByRestaurant(1L, "PENDING", pageable, 1L, "RESTAURANT_OWNER");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getCustomerOrderStats_Success() {
        // Arrange
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByCustomerId(1L)).thenReturn(orders);

        // Act
        OrderStatsDTO result = orderService.getCustomerOrderStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTotalOrders());
        verify(orderRepository).findByCustomerId(1L);
    }

    @Test
    void getRestaurantOrderStats_Success() {
        // Arrange
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByRestaurantId(1L)).thenReturn(orders);

        // Mock restaurant service for restaurant owner verification
        when(restaurantServiceClient.getRestaurantByOwnerId(1L)).thenReturn(restaurantResponse);

        // Act
        OrderStatsDTO result = orderService.getRestaurantOrderStats(1L, 1L, "RESTAURANT_OWNER");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTotalOrders());
        verify(orderRepository).findByRestaurantId(1L);
    }

    @Test
    void rateOrder_Success() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED); // Only delivered orders can be rated
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Act
        OrderDTO result = orderService.rateOrder(1L, 1L, 5, "Great food!");

        // Assert
        assertNotNull(result);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderEventPublisher).publishOrderRatedEvent(any(Order.class));
    }

    @Test
    void rateOrder_UnauthorizedCustomer() {
        // Arrange
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
                orderService.rateOrder(1L, 2L, 5, "Great food!")); // Different customer ID
    }

    @Test
    void rateOrder_OrderNotDelivered() {
        // Arrange
        order.setStatus(OrderStatus.PENDING); // Not delivered
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.rateOrder(1L, 1L, 5, "Great food!"));
    }

    @Test
    void fallbackCreateOrder_ThrowsBusinessException() {
        // Arrange
        Exception cause = new RuntimeException("Service unavailable");

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                orderService.fallbackCreateOrder(createOrderRequest, 1L, "customer@test.com", cause));
    }
}