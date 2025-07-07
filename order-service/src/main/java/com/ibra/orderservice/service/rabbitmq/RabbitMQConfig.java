package com.ibra.orderservice.service.rabbitmq;

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

    // Routing keys (Order Service is the producer)
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
     * Queue for the Notification Service to consume events.
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true); // Durable queue
    }

    /**
     * Queue for the Restaurant Service to consume events.
     */
    @Bean
    public Queue restaurantQueue() {
        return new Queue(RESTAURANT_QUEUE, true); // Durable queue
    }

    /**
     * Binds the Notification Queue to the Order Exchange.
     * Notification service listens to all events under "order.event."
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with("order.event.#"); // Wildcard routing key for all order events
    }

    /**
     * Binds the Restaurant Queue to the Order Exchange for 'ORDER_PLACED' events.
     */
    @Bean
    public Binding restaurantBindingPlaced(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_PLACED);
    }

    /**
     * Binds the Restaurant Queue to the Order Exchange for 'ORDER_STATUS_UPDATED' events.
     */
    @Bean
    public Binding restaurantBindingStatusUpdated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_STATUS_UPDATED);
    }

    @Bean
    public Binding restaurantBindingCancelled(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_CANCELLED);
    }

    @Bean
    public Binding restaurantBindingRated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_RATED);
    }


    /**
     * JSON Message Converter for object serialization/deserialization.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures RabbitTemplate with the JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}