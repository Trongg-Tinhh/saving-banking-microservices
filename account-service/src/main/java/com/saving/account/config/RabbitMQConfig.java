package com.saving.account.config;

import com.saving.account.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange accountEventsExchange() {
        return ExchangeBuilder
                .topicExchange(Constants.ACCOUNT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue accountCreatedQueue() {
        return QueueBuilder.durable(Constants.ACCOUNT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.ACCOUNT_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue accountDebitedQueue() {
        return QueueBuilder.durable(Constants.ACCOUNT_DEBITED_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.ACCOUNT_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue accountCreditedQueue() {
        return QueueBuilder.durable(Constants.ACCOUNT_CREDITED_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.ACCOUNT_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue accountStatusQueue() {
        return QueueBuilder.durable(Constants.ACCOUNT_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.ACCOUNT_EXCHANGE + ".dlx")
                .build();
    }

    @Bean public Binding accountCreatedBinding(Queue accountCreatedQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(accountCreatedQueue).to(accountEventsExchange).with("account.created.*");
    }
    @Bean public Binding accountDebitedBinding(Queue accountDebitedQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(accountDebitedQueue).to(accountEventsExchange).with("account.debit.*");
    }
    @Bean public Binding accountCreditedBinding(Queue accountCreditedQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(accountCreditedQueue).to(accountEventsExchange).with("account.credit.*");
    }
    @Bean public Binding accountStatusBinding(Queue accountStatusQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(accountStatusQueue).to(accountEventsExchange).with("account.status.*");
    }

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
