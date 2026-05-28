package com.saving.account.service;

import com.saving.account.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishAccountCreated(String accountNo, String cif, String accountType) {
        Map<String, Object> event = buildBase("ACCOUNT_CREATED", accountNo, cif);
        event.put("accountType", accountType);
        publish(Constants.ACCOUNT_EXCHANGE, Constants.ROUTING_ACCOUNT_CREATED, event);
        log.info("Published ACCOUNT_CREATED: accountNo={}", accountNo);
    }

    public void publishDebit(String accountNo, String cif, BigDecimal amount,
                             BigDecimal availableBalance, String reference) {
        Map<String, Object> event = buildBase("ACCOUNT_DEBITED", accountNo, cif);
        event.put("amount",           amount);
        event.put("availableBalance", availableBalance);
        event.put("reference",        reference);
        publish(Constants.ACCOUNT_EXCHANGE, Constants.ROUTING_ACCOUNT_DEBIT, event);
    }

    public void publishCredit(String accountNo, String cif, BigDecimal amount,
                              BigDecimal availableBalance, String reference) {
        Map<String, Object> event = buildBase("ACCOUNT_CREDITED", accountNo, cif);
        event.put("amount",           amount);
        event.put("availableBalance", availableBalance);
        event.put("reference",        reference);
        publish(Constants.ACCOUNT_EXCHANGE, Constants.ROUTING_ACCOUNT_CREDIT, event);
    }

    public void publishStatusChanged(String accountNo, String cif, String oldStatus, String newStatus) {
        Map<String, Object> event = buildBase("ACCOUNT_STATUS_CHANGED", accountNo, cif);
        event.put("oldStatus", oldStatus);
        event.put("newStatus", newStatus);
        publish(Constants.ACCOUNT_EXCHANGE, Constants.ROUTING_ACCOUNT_STATUS, event);
    }

    private Map<String, Object> buildBase(String eventType, String accountNo, String cif) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",     eventType);
        event.put("accountNo",     accountNo);
        event.put("cif",           cif);
        event.put("timestamp",     Instant.now().toString());
        event.put("correlationId", MDC.get(Constants.CORRELATION_ID_MDC_KEY));
        event.put("source",        "account-service");
        return event;
    }

    private void publish(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception ex) {
            log.error("Failed to publish event to exchange={}, routing={}: {}",
                    exchange, routingKey, ex.getMessage());
        }
    }
}
