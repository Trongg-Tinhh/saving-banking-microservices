package com.saving.auth.config;

import com.saving.auth.common.Constants;
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
    public TopicExchange authEventsExchange() {
        return ExchangeBuilder
                .topicExchange(Constants.AUTH_EXCHANGE)
                .durable(true)
                .build();
    }

    // ── Queues ────────────────────────────────────────────────────
    @Bean
    public Queue authLoginQueue() {
        return QueueBuilder
                .durable(Constants.AUTH_LOGIN_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.AUTH_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue authLogoutQueue() {
        return QueueBuilder
                .durable(Constants.AUTH_LOGOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.AUTH_EXCHANGE + ".dlx")
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────
    @Bean
    public Binding loginBinding(Queue authLoginQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(authLoginQueue)
                .to(authEventsExchange)
                .with("auth.login.*");
    }

    @Bean
    public Binding logoutBinding(Queue authLogoutQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(authLogoutQueue)
                .to(authEventsExchange)
                .with("auth.logout.*");
    }

    // ── Message Converter (JSON) ──────────────────────────────────
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
