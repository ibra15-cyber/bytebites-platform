package com.ibra.orderservice.controller;


import com.ibra.dto.ApiResponse;
import com.ibra.orderservice.dto.*;
import com.ibra.orderservice.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Creating order for restaurant: {}", request.getRestaurantId());

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));
        String customerEmail = httpRequest.getHeader("X-User-Email");

        OrderDTO orderDTO = orderService.createOrder(request, customerId, customerEmail);

        ApiResponse<OrderDTO> response = ApiResponse.<OrderDTO>builder()
                .success(true)
                .message("Order created successfully")
                .data(orderDTO)
                .build();

        logger.info("Order created with ID: {}", orderDTO.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getCustomerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderDTO> orders = orderService.getOrdersByCustomer(customerId, pageable);

        ApiResponse<Page<OrderDTO>> response = ApiResponse.<Page<OrderDTO>>builder()
                .success(true)
                .message("Orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('CUSTOMER') or hasAuthority('RESTAURANT_OWNER') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));
        String userRole = httpRequest.getHeader("X-User-Role");

        OrderDTO orderDTO = orderService.getOrderById(orderId, customerId, userRole);

        ApiResponse<OrderDTO> response = ApiResponse.<OrderDTO>builder()
                .success(true)
                .message("Order retrieved successfully")
                .data(orderDTO)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));

        OrderDTO orderDTO = orderService.cancelOrder(orderId, customerId);

        ApiResponse<OrderDTO> response = ApiResponse.<OrderDTO>builder()
                .success(true)
                .message("Order cancelled successfully")
                .data(orderDTO)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest httpRequest) {

        Long userId = Long.valueOf(httpRequest.getHeader("X-User-Id"));
        String userRole = httpRequest.getHeader("X-User-Role");

        OrderDTO orderDTO = orderService.updateOrderStatus(orderId, request.getStatus(), userId, userRole);

        ApiResponse<OrderDTO> response = ApiResponse.<OrderDTO>builder()
                .success(true)
                .message("Order status updated successfully")
                .data(orderDTO)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getRestaurantOrders(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {

        Long userId = Long.valueOf(httpRequest.getHeader("X-User-Id"));
        String userRole = httpRequest.getHeader("X-User-Role");

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderDTO> orders = orderService.getOrdersByRestaurant(restaurantId, status, pageable, userId, userRole);

        ApiResponse<Page<OrderDTO>> response = ApiResponse.<Page<OrderDTO>>builder()
                .success(true)
                .message("Restaurant orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/customer")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getCustomerOrderStats(
            HttpServletRequest httpRequest) {

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));

        OrderStatsDTO stats = orderService.getCustomerOrderStats(customerId);

        ApiResponse<OrderStatsDTO> response = ApiResponse.<OrderStatsDTO>builder()
                .success(true)
                .message("Order statistics retrieved successfully")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/restaurant/{restaurantId}")
    @PreAuthorize("hasAuthority('RESTAURANT_OWNER') or hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getRestaurantOrderStats(
            @PathVariable Long restaurantId,
            HttpServletRequest httpRequest) {

        Long userId = Long.valueOf(httpRequest.getHeader("X-User-Id"));
        String userRole = httpRequest.getHeader("X-User-Role");

        OrderStatsDTO stats = orderService.getRestaurantOrderStats(restaurantId, userId, userRole);

        ApiResponse<OrderStatsDTO> response = ApiResponse.<OrderStatsDTO>builder()
                .success(true)
                .message("Restaurant order statistics retrieved successfully")
                .data(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/rating")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> rateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody RateOrderRequest request,
            HttpServletRequest httpRequest) {

        Long customerId = Long.valueOf(httpRequest.getHeader("X-User-Id"));

        OrderDTO orderDTO = orderService.rateOrder(orderId, customerId, request.getRating(), request.getReview());

        ApiResponse<OrderDTO> response = ApiResponse.<OrderDTO>builder()
                .success(true)
                .message("Order rated successfully")
                .data(orderDTO)
                .build();

        return ResponseEntity.ok(response);
    }
}