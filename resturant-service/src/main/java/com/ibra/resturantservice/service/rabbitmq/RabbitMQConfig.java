package com.ibra.resturantservice.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange name
    public static final String ORDER_EXCHANGE = "order.topic.exchange";

    // Queue names
    public static final String NOTIFICATION_QUEUE = "notification.order.queue";
    public static final String RESTAURANT_QUEUE = "restaurant.order.queue";

//     Routing keys
    public static final String NOTIFICATION_ROUTING_KEY = "order.notification";
    public static final String RESTAURANT_ROUTING_KEY = "order.restaurant";
    public static final String ORDER_ROUTING_KEY_PLACED = "order.event.placed";
    public static final String ORDER_ROUTING_KEY_CANCELLED = "order.event.cancelled";
    public static final String ORDER_ROUTING_KEY_STATUS_UPDATED = "order.event.status.updated";
    public static final String ORDER_ROUTING_KEY_RATED = "order.event.rated";


    /**
     * Declares a Topic Exchange for flexible routing
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    /**
     * Queue for the Notification Service
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    /**
     * Queue for the Restaurant Service
     */
    @Bean
    public Queue restaurantQueue() {
        return new Queue(RESTAURANT_QUEUE, true);
    }

    /**
     * Binds the Notification Queue to the Order Exchange
     *  Bindings: Notification service listens to all order events
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with("order.event.#"); // Listen to all events under "order.event."
    }

    // Bindings: Restaurant service listens to placed and status updated events
    @Bean
    public Binding restaurantBindingPlaced(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_PLACED);
    }
    @Bean
    public Binding restaurantBindingStatusUpdated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_STATUS_UPDATED);
    }
    /**
     * Binds the Restaurant Queue to the Order Exchange
     */
    @Bean
    public Binding restaurantBinding(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(RESTAURANT_ROUTING_KEY);
    }

    /**
     * JSON Message Converter for object serialization
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}