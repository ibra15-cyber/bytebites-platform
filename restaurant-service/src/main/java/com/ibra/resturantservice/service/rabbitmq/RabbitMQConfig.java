package com.ibra.resturantservice.service.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Inject properties from application.yml
    @Value("${app.rabbitmq.order-exchange-name}")
    private String orderExchangeName;

    @Value("${app.rabbitmq.restaurant-queue-name}")
    private String restaurantQueueName;

    @Value("${app.rabbitmq.order-routing-key-placed}")
    private String orderRoutingKeyPlaced;

    @Value("${app.rabbitmq.order-routing-key-status-updated}")
    private String orderRoutingKeyStatusUpdated;

    @Value("${app.rabbitmq.order-routing-key-cancelled}")
    private String orderRoutingKeyCancelled;

    @Value("${app.rabbitmq.order-routing-key-rated}")
    private String orderRoutingKeyRated;


    @Bean
    public TopicExchange orderExchange() {
        // Declares the exchange that the Order Service publishes to.
        // It must match the exchange declared by the producer.
        return new TopicExchange(orderExchangeName); // Use injected value
    }

    @Bean
    public Queue restaurantQueue() {
        // Declares this service's specific queue.
        return new Queue(restaurantQueueName, true); // Use injected value, 'true' for durable
    }

    // Bindings for the restaurant queue to the order exchange for specific events
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