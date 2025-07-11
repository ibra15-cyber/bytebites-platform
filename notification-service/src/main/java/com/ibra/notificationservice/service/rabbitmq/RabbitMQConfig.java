package com.ibra.notificationservice.service.rabbitmq;

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

    @Value("${app.rabbitmq.notification-queue-name}")
    private String notificationQueueName;

    @Value("${app.rabbitmq.notification-routing-key-all-orders}")
    private String notificationRoutingKeyAllOrders;


    @Bean
    public TopicExchange orderExchange() {
        // Declares the exchange that the Order Service publishes to.
        // It must match the exchange declared by the producer.
        return new TopicExchange(orderExchangeName); // Use injected value
    }

    @Bean
    public Queue notificationQueue() {
        // Declares this service's specific queue.
        return new Queue(notificationQueueName, true); // Use injected value, 'true' for durable
    }

    /**
     * Bind notification queue to ALL order events using wildcard pattern
     * This will receive all events that start with "order.event."
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(orderExchange())
                .with(notificationRoutingKeyAllOrders); // Use injected value
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}