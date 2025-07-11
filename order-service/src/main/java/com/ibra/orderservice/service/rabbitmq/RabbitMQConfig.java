package com.ibra.orderservice.service.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class RabbitMQConfig {

    // Inject properties from application.yml
    @Value("${app.rabbitmq.order-exchange-name}")
    private String orderExchangeName;

    @Value("${app.rabbitmq.notification-queue-name}")
    private String notificationQueueName;

    @Value("${app.rabbitmq.restaurant-queue-name}")
    private String restaurantQueueName;

    @Value("${app.rabbitmq.order-routing-key-placed}")
    private String orderRoutingKeyPlaced;

    @Value("${app.rabbitmq.order-routing-key-cancelled}")
    private String orderRoutingKeyCancelled;

    @Value("${app.rabbitmq.order-routing-key-status-updated}")
    private String orderRoutingKeyStatusUpdated;

    @Value("${app.rabbitmq.order-routing-key-rated}")
    private String orderRoutingKeyRated;


    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchangeName); // Use injected value
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(notificationQueueName, true); // Use injected value
    }

    @Bean
    public Queue restaurantQueue() {
        return new Queue(restaurantQueueName, true); // Use injected value
    }

    // Bindings for notification queue
    @Bean
    public Binding notificationBindingPlaced(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with(orderRoutingKeyPlaced); // Use injected value
    }

    @Bean
    public Binding notificationBindingStatusUpdated(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with(orderRoutingKeyStatusUpdated); // Use injected value
    }

    @Bean
    public Binding notificationBindingCancelled(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with(orderRoutingKeyCancelled); // Use injected value
    }

    @Bean
    public Binding notificationBindingRated(Queue notificationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(orderExchange)
                .with(orderRoutingKeyRated); // Use injected value
    }

    // Bindings for restaurant queue
    @Bean
    public Binding restaurantBindingPlaced(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(orderRoutingKeyPlaced); // Use injected value
    }

    @Bean
    public Binding restaurantBindingStatusUpdated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(orderRoutingKeyStatusUpdated); // Use injected value
    }

    @Bean
    public Binding restaurantBindingCancelled(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(orderRoutingKeyCancelled); // Use injected value
    }

    @Bean
    public Binding restaurantBindingRated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(orderRoutingKeyRated); // Use injected value
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