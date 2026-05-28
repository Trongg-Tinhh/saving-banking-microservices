package com.saving.customer.config;

import com.saving.customer.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────────────────
    @Bean
    public TopicExchange customerEventsExchange() {
        return ExchangeBuilder
                .topicExchange(Constants.CUSTOMER_EXCHANGE)
                .durable(true)
                .build();
    }

    // ── Queues ────────────────────────────────────────────────────
    @Bean
    public Queue customerCreatedQueue() {
        return QueueBuilder
                .durable(Constants.CUSTOMER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.CUSTOMER_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue customerUpdatedQueue() {
        return QueueBuilder
                .durable(Constants.CUSTOMER_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.CUSTOMER_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue customerKycQueue() {
        return QueueBuilder
                .durable(Constants.CUSTOMER_KYC_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.CUSTOMER_EXCHANGE + ".dlx")
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────
    @Bean
    public Binding customerCreatedBinding(Queue customerCreatedQueue, TopicExchange customerEventsExchange) {
        return BindingBuilder.bind(customerCreatedQueue)
                .to(customerEventsExchange)
                .with("customer.created.*");
    }

    @Bean
    public Binding customerUpdatedBinding(Queue customerUpdatedQueue, TopicExchange customerEventsExchange) {
        return BindingBuilder.bind(customerUpdatedQueue)
                .to(customerEventsExchange)
                .with("customer.updated.*");
    }

    @Bean
    public Binding customerKycBinding(Queue customerKycQueue, TopicExchange customerEventsExchange) {
        return BindingBuilder.bind(customerKycQueue)
                .to(customerEventsExchange)
                .with("customer.kyc.*");
    }

    // ── Converter & Template ──────────────────────────────────────
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }
}
