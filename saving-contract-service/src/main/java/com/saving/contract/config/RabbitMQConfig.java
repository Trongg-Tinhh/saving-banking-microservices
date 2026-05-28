package com.saving.contract.config;

import com.saving.contract.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ── Dead-letter exchange ─────────────────────────────────────────────────

    @Bean
    public DirectExchange contractDlx() {
        return new DirectExchange(Constants.Rabbit.CONTRACT_DLX, true, false);
    }

    @Bean
    public Queue contractDlq() {
        return QueueBuilder.durable(Constants.Rabbit.CONTRACT_DLQ).build();
    }

    @Bean
    public Binding contractDlqBinding() {
        return BindingBuilder.bind(contractDlq())
                .to(contractDlx())
                .with(Constants.Rabbit.CONTRACT_DLQ);
    }

    // ── Main exchange ────────────────────────────────────────────────────────

    @Bean
    public TopicExchange contractExchange() {
        return ExchangeBuilder
                .topicExchange(Constants.Rabbit.CONTRACT_EXCHANGE)
                .durable(true)
                .build();
    }

    // ── Queues ───────────────────────────────────────────────────────────────

    @Bean
    public Queue contractOpenedQueue() {
        return buildQueueWithDlx(Constants.Rabbit.CONTRACT_OPENED_QUEUE);
    }

    @Bean
    public Queue contractClosedQueue() {
        return buildQueueWithDlx(Constants.Rabbit.CONTRACT_CLOSED_QUEUE);
    }

    @Bean
    public Queue contractMaturedQueue() {
        return buildQueueWithDlx(Constants.Rabbit.CONTRACT_MATURED_QUEUE);
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding contractOpenedBinding() {
        return BindingBuilder.bind(contractOpenedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_OPENED_KEY);
    }

    @Bean
    public Binding contractClosedBinding() {
        return BindingBuilder.bind(contractClosedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_CLOSED_KEY);
    }

    @Bean
    public Binding contractMaturedBinding() {
        return BindingBuilder.bind(contractMaturedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_MATURED_KEY);
    }

    // ── JSON converter ───────────────────────────────────────────────────────

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

    // ── Helper ───────────────────────────────────────────────────────────────

    private Queue buildQueueWithDlx(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", Constants.Rabbit.CONTRACT_DLX);
        args.put("x-dead-letter-routing-key", Constants.Rabbit.CONTRACT_DLQ);
        return new Queue(queueName, true, false, false, args);
    }
}
