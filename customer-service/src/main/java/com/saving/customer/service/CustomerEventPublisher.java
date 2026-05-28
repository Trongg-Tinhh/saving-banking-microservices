package com.saving.customer.service;

import com.saving.customer.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    // ── Customer Created ──────────────────────────────────────────

    public void publishCustomerCreated(String cif, String fullName) {
        Map<String, Object> event = buildBaseEvent("CUSTOMER_CREATED", cif);
        event.put("fullName", fullName);
        publish(Constants.CUSTOMER_EXCHANGE, Constants.ROUTING_CUSTOMER_CREATED, event);
        log.info("Published CUSTOMER_CREATED event: cif={}", cif);
    }

    // ── Customer Updated ──────────────────────────────────────────

    public void publishCustomerUpdated(String cif, String fullName) {
        Map<String, Object> event = buildBaseEvent("CUSTOMER_UPDATED", cif);
        event.put("fullName", fullName);
        publish(Constants.CUSTOMER_EXCHANGE, Constants.ROUTING_CUSTOMER_UPDATED, event);
        log.info("Published CUSTOMER_UPDATED event: cif={}", cif);
    }

    // ── KYC Status Changed ────────────────────────────────────────

    public void publishKycStatusChanged(String cif, String oldStatus, String newStatus, String verifiedBy) {
        Map<String, Object> event = buildBaseEvent("CUSTOMER_KYC_UPDATED", cif);
        event.put("oldStatus",  oldStatus);
        event.put("newStatus",  newStatus);
        event.put("verifiedBy", verifiedBy);
        publish(Constants.CUSTOMER_EXCHANGE, Constants.ROUTING_CUSTOMER_KYC, event);
        log.info("Published CUSTOMER_KYC_UPDATED event: cif={}, {}→{}", cif, oldStatus, newStatus);
    }

    // ── Private helpers ────────────────────────────────────────────

    private Map<String, Object> buildBaseEvent(String eventType, String cif) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType",     eventType);
        event.put("cif",           cif);
        event.put("timestamp",     Instant.now().toString());
        event.put("correlationId", MDC.get(Constants.CORRELATION_ID_MDC_KEY));
        event.put("source",        "customer-service");
        return event;
    }

    private void publish(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        } catch (Exception ex) {
            // Log and swallow — event publishing failure should not break the main transaction
            log.error("Failed to publish event to exchange={} routing={}: {}",
                    exchange, routingKey, ex.getMessage());
        }
    }
}
