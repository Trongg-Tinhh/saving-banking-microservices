package com.saving.transaction.config;

import com.saving.transaction.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQConfig {

    // ── Dead-letter exchange / queue ─────────────────────────────────────────

    @Bean
    public DirectExchange txDlx() {
        return new DirectExchange(Constants.Rabbit.TX_DLX, true, false);
    }

    @Bean
    public Queue txDlq() {
        return QueueBuilder.durable(Constants.Rabbit.TX_DLQ).build();
    }

    @Bean
    public Binding txDlqBinding() {
        return BindingBuilder.bind(txDlq()).to(txDlx()).with(Constants.Rabbit.TX_DLQ);
    }

    // ── Source exchange (owned by contract-service, we just bind to it) ──────

    /**
     * Declare the exchange so the app can start even if contract-service
     * hasn't created it yet. PassiveDeclare would be cleaner in production.
     */
    @Bean
    public TopicExchange contractExchange() {
        return ExchangeBuilder
                .topicExchange(Constants.Rabbit.CONTRACT_EXCHANGE)
                .durable(true)
                .build();
    }

    // ── Our consumer queues ──────────────────────────────────────────────────

    @Bean
    public Queue txContractOpenedQueue() {
        return buildWithDlx(Constants.Rabbit.TX_CONTRACT_OPENED_QUEUE);
    }

    @Bean
    public Queue txContractClosedQueue() {
        return buildWithDlx(Constants.Rabbit.TX_CONTRACT_CLOSED_QUEUE);
    }

    @Bean
    public Queue txContractMaturedQueue() {
        return buildWithDlx(Constants.Rabbit.TX_CONTRACT_MATURED_QUEUE);
    }

    // ── Bindings to contract-service exchange ────────────────────────────────

    @Bean
    public Binding txContractOpenedBinding() {
        return BindingBuilder.bind(txContractOpenedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_OPENED_KEY);
    }

    @Bean
    public Binding txContractClosedBinding() {
        return BindingBuilder.bind(txContractClosedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_CLOSED_KEY);
    }

    @Bean
    public Binding txContractMaturedBinding() {
        return BindingBuilder.bind(txContractMaturedQueue())
                .to(contractExchange())
                .with(Constants.Rabbit.CONTRACT_MATURED_KEY);
    }

    // ── Message converter ────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter());
        return t;
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Queue buildWithDlx(String name) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange",    Constants.Rabbit.TX_DLX);
        args.put("x-dead-letter-routing-key", Constants.Rabbit.TX_DLQ);
        return new Queue(name, true, false, false, args);
    }
}
