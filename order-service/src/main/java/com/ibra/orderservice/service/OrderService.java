// Complete OrderService.java
package com.ibra.orderservice.service;

import com.ibra.dto.ApiResponse;
import com.ibra.dto.MenuItemDTO;
import com.ibra.dto.RestaurantDTO;
import com.ibra.exception.BusinessException;
import com.ibra.exception.ResourceNotFoundException;
import com.ibra.exception.UnauthorizedException;
import com.ibra.orderservice.dto.*;
import com.ibra.orderservice.entity.Order;
import com.ibra.orderservice.entity.OrderItem;
import com.ibra.orderservice.enums.OrderStatus;
import com.ibra.orderservice.mapper.OrderMapper;
import com.ibra.orderservice.repository.OrderItemRepository;
import com.ibra.orderservice.repository.OrderRepository;
import com.ibra.orderservice.service.external.RestaurantServiceClient;
import com.ibra.orderservice.service.rabbitmq.OrderEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RestaurantServiceClient restaurantServiceClient;

    @Autowired
    private OrderEventPublisher orderEventPublisher;




    // Create new order
    @CircuitBreaker(name = "restaurant-service", fallbackMethod = "fallbackCreateOrder")
    public OrderDTO createOrder(CreateOrderRequest request, Long customerId, String customerEmail) {
        logger.info("Creating new order for customer: {} at restaurant: {}", customerId, request.getRestaurantId());

        // Validate restaurant and get restaurant details
        ApiResponse<RestaurantDTO> restaurantResponse = restaurantServiceClient.getRestaurantById(request.getRestaurantId());
        if (restaurantResponse == null || !restaurantResponse.isSuccess() || restaurantResponse.getData() == null) {
            throw new ResourceNotFoundException("Restaurant not found with ID: " + request.getRestaurantId());
        }
        RestaurantDTO restaurant = restaurantResponse.getData();

        System.out.println("menuItem request id: " + request.getOrderItems().getFirst().getMenuItemId());
        ApiResponse<MenuItemDTO> menuItemResponse = restaurantServiceClient.getMenuItemById(request.getOrderItems().getFirst().getMenuItemId());
        if (menuItemResponse != null && menuItemResponse.isSuccess() && menuItemResponse.getData() != null) {
            System.out.println("menu item after fetching from restaurant service: " + menuItemResponse.getData().getName());
        }
        System.out.println("restaurant returns with dto; " + restaurant);

        System.out.println(restaurant.getId() + " " + request.getRestaurantId());

        // Validate menu items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            ApiResponse<MenuItemDTO> response = restaurantServiceClient.getMenuItemById(itemRequest.getMenuItemId());
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("Invalid menu item: " + itemRequest.getMenuItemId());
            }
            MenuItemDTO menuItem = response.getData();

            if (!menuItem.getRestaurantId().equals(request.getRestaurantId())) {
                throw new BusinessException("Invalid menu item: " + itemRequest.getMenuItemId());
            }
            totalAmount = totalAmount.add(menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        // Rest of your code remains the same...
        // Create order
        Order order = new Order(
                customerId,
                request.getRestaurantId(),
                restaurant.getName(),
                customerEmail,
                totalAmount,
                request.getDeliveryAddress(),//        order.setRestaurantEmail(restaurant.getEmail());

                request.getDeliveryPhone()
        );

        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(45));

        Order savedOrder = orderRepository.save(order);

        // Create order items
        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(itemRequest -> {
                    ApiResponse<MenuItemDTO> response = restaurantServiceClient.getMenuItemById(itemRequest.getMenuItemId());
                    MenuItemDTO menuItem = response.getData(); // This should now work
                    OrderItem orderItem = new OrderItem(
                            savedOrder,
                            itemRequest.getMenuItemId(),
                            menuItem.getName(),
                            itemRequest.getQuantity(),
                            menuItem.getPrice()
                    );
                    orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());
                    return orderItem;
                })
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);
        savedOrder.setOrderItems(orderItems);

        orderEventPublisher.publishOrderPlacedEvent(savedOrder);

        logger.info("Order created successfully with ID: {}", savedOrder.getId());
        return orderMapper.toDTO(savedOrder);
    }
    // Get orders by customer with pagination
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByCustomer(Long customerId, Pageable pageable) {
        logger.info("Fetching orders for customer: {}", customerId);

        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return orders.map(orderMapper::toDTO);
    }

    // Get order by ID with authorization check
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, Long userId, String userRole) {
        logger.info("Fetching order: {} for user: {} with role: {}", orderId, userId, userRole);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Authorization check
        if ("CUSTOMER".equals(userRole) && !order.getCustomerId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this order");
        } else if ("RESTAURANT_OWNER".equals(userRole)) {
            // Verify restaurant ownership
            if (!order.getRestaurantId().equals(getRestaurantIdForOwner(userId))) {
                throw new UnauthorizedException("You are not authorized to view this order");
            }
        }

        return orderMapper.toDTO(order);
    }

    // Cancel order
    public OrderDTO cancelOrder(Long orderId, Long customerId) {
        logger.info("Cancelling order: {} for customer: {}", orderId, customerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Verify ownership
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("You are not authorized to cancel this order");
        }

        // Check if order can be cancelled
        if (!canBeCancelled(order.getStatus())) {
            throw new BusinessException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Publish order cancelled event
        orderEventPublisher.publishOrderCancelledEvent(savedOrder);

        logger.info("Order cancelled successfully: {}", orderId);
        return orderMapper.toDTO(savedOrder);
    }

    // Update order status
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus, Long userId, String userRole) {
        logger.info("Updating order status: {} to {} by user: {}", orderId, newStatus, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Authorization check for restaurant owner
        if ("RESTAURANT_OWNER".equals(userRole)) {
            // Verify restaurant ownership
            if (!order.getRestaurantId().equals(getRestaurantIdForOwner(userId))) {
                throw new UnauthorizedException("You are not authorized to update this order");
            }
        }

        // Validate status transition
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new BusinessException("Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Update estimated delivery time for certain statuses
        if (newStatus == OrderStatus.PREPARING) {
            order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(30));
        } else if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(15));
        }

        Order savedOrder = orderRepository.save(order);

        // Publish order status updated event
        orderEventPublisher.publishOrderStatusUpdatedEvent(savedOrder);

        logger.info("Order status updated successfully: {} to {}", orderId, newStatus);
        return orderMapper.toDTO(savedOrder);
    }

    // Get orders by restaurant
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByRestaurant(Long restaurantId, String status, Pageable pageable, Long userId, String userRole) {
        logger.info("Fetching orders for restaurant: {} with status: {}", restaurantId, status);

        // Authorization check for restaurant owner
        if ("RESTAURANT_OWNER".equals(userRole)) {
            if (!restaurantId.equals(getRestaurantIdForOwner(userId))) {
                throw new UnauthorizedException("You are not authorized to view orders for this restaurant");
            }
        }

        Specification<Order> spec = Specification.where(null);
        spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("restaurantId"), restaurantId));

        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), orderStatus));
        }

        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return orders.map(orderMapper::toDTO);
    }

    // Get customer order statistics
    @Transactional(readOnly = true)
    public OrderStatsDTO getCustomerOrderStats(Long customerId) {
        logger.info("Fetching order statistics for customer: {}", customerId);

        List<Order> orders = orderRepository.findByCustomerId(customerId);

        OrderStatsDTO stats = new OrderStatsDTO();
        stats.setTotalOrders((long) orders.size());
        stats.setCompletedOrders(orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count());
        stats.setCancelledOrders(orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                .count());
        stats.setPendingOrders(orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                .count());

        BigDecimal totalSpent = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSpent(totalSpent);

        // Calculate average rating
        OptionalDouble avgRating = orders.stream()
                .filter(order -> order.getRating() != null)
                .mapToInt(Order::getRating)
                .average();
        stats.setAverageRating(avgRating.isPresent() ? avgRating.getAsDouble() : null);

        // Find favorite restaurant
        String favoriteRestaurant = orders.stream()
                .collect(Collectors.groupingBy(Order::getRestaurantName, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        stats.setFavoriteRestaurant(favoriteRestaurant);

        return stats;
    }

    // Get restaurant order statistics
    @Transactional(readOnly = true)
    public OrderStatsDTO getRestaurantOrderStats(Long restaurantId, Long userId, String userRole) {
        logger.info("Fetching order statistics for restaurant: {}", restaurantId);

        // Authorization check for restaurant owner
        if ("RESTAURANT_OWNER".equals(userRole)) {
            if (!restaurantId.equals(getRestaurantIdForOwner(userId))) {
                throw new UnauthorizedException("You are not authorized to view statistics for this restaurant");
            }
        }

        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);

        OrderStatsDTO stats = new OrderStatsDTO();
        stats.setTotalOrders((long) orders.size());
        stats.setCompletedOrders(orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .count());
        stats.setCancelledOrders(orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                .count());
        stats.setPendingOrders(orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED)
                .count());

        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSpent(totalRevenue);

        // Calculate average rating
        OptionalDouble avgRating = orders.stream()
                .filter(order -> order.getRating() != null)
                .mapToInt(Order::getRating)
                .average();
        stats.setAverageRating(avgRating.isPresent() ? avgRating.getAsDouble() : null);

        return stats;
    }

    // Rate order
    public OrderDTO rateOrder(Long orderId, Long customerId, Integer rating, String review) {
        logger.info("Rating order: {} by customer: {} with rating: {}", orderId, customerId, rating);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Verify ownership
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("You are not authorized to rate this order");
        }

        // Check if order is eligible for rating
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Only delivered orders can be rated");
        }

        order.setRating(rating);
        order.setReview(review);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Publish order rated event
        orderEventPublisher.publishOrderRatedEvent(savedOrder);

        logger.info("Order rated successfully: {}", orderId);
        return orderMapper.toDTO(savedOrder);
    }

    // Fallback method for circuit breaker
    public OrderDTO fallbackCreateOrder(CreateOrderRequest request, Long customerId, String customerEmail, Exception ex) {
        logger.error("Fallback method called for createOrder due to: {}", ex.getMessage());
        throw new BusinessException("Restaurant service is currently unavailable. Please try again later.");
    }

    // Helper methods
    private boolean canBeCancelled(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED:
                return newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
            case PREPARING:
                return newStatus == OrderStatus.READY_FOR_PICKUP || newStatus == OrderStatus.OUT_FOR_DELIVERY;
            case READY_FOR_PICKUP:
                return newStatus == OrderStatus.OUT_FOR_DELIVERY || newStatus == OrderStatus.DELIVERED;
            case OUT_FOR_DELIVERY:
                return newStatus == OrderStatus.DELIVERED;
            case DELIVERED:
            case CANCELLED:
                return false; // Terminal states
            default:
                return false;
        }
    }

    private Long getRestaurantIdForOwner(Long userId) {
        // This should call restaurant service to get restaurant ID for the owner
        try {
            ApiResponse<RestaurantDTO> response = restaurantServiceClient.getRestaurantByOwnerId(userId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().getId();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching restaurant for owner: {}", userId, e);
            return null;
        }
    }
}