package com.ibra.resturantservice.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange; // Use TopicExchange as defined in Order Service
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // These constants must match the exchange name used by the Order Service (publisher)
    // and the queue name configured in restaurant-service.yml for this consumer.
    public static final String ORDER_EXCHANGE = "order.topic.exchange"; // Must match Order Service's exchange name
    public static final String RESTAURANT_QUEUE = "restaurant.order.queue"; // This service's dedicated queue name

    // Routing keys this service is interested in (must match publisher's keys)
    public static final String ORDER_ROUTING_KEY_PLACED = "order.event.placed";
    public static final String ORDER_ROUTING_KEY_STATUS_UPDATED = "order.event.status.updated";

    /**
     * Declares the Topic Exchange. This service needs to declare it locally
     * to bind its queue to it, even if the Order Service is the primary creator.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    /**
     * Declares the queue for the Restaurant Service.
     * This queue will receive messages routed from the order exchange.
     */
    @Bean
    public Queue restaurantQueue() {
        return new Queue(RESTAURANT_QUEUE, true); // Durable queue
    }

    /**
     * Binds the Restaurant Queue to the Order Exchange for 'order.event.placed' messages.
     * This ensures the restaurant service receives new order events.
     */
    @Bean
    public Binding restaurantBindingPlaced(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_PLACED);
    }

    /**
     * Binds the Restaurant Queue to the Order Exchange for 'order.event.status.updated' messages.
     * This ensures the restaurant service receives order status updates.
     */
    @Bean
    public Binding restaurantBindingStatusUpdated(Queue restaurantQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(restaurantQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY_STATUS_UPDATED);
    }

    /**
     * Configures a MessageConverter to use Jackson for JSON serialization/deserialization.
     * This is crucial for sending/receiving Java objects as JSON messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Customizes the RabbitTemplate to use the Jackson2JsonMessageConverter.
     * While this service is primarily a consumer, having a properly configured
     * RabbitTemplate is good practice and might be needed if it publishes
     * its own events later (e.g., OrderPreparationStartedEvent).
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Configures the SimpleRabbitListenerContainerFactory for message consumption.
     * This ensures the listener uses the correct connection factory and message converter.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}