package com.saving.contract.service;

import com.saving.contract.common.Constants;
import com.saving.contract.entity.SavingContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishContractOpened(SavingContract contract) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType",       "CONTRACT_OPENED");
        payload.put("contractNo",      contract.getContractNo());
        payload.put("cif",             contract.getCif());
        payload.put("productCode",     contract.getProductCode());
        payload.put("termId",          contract.getTermId());
        payload.put("principalAmount", contract.getPrincipalAmount());
        payload.put("interestRate",    contract.getInterestRate());
        payload.put("currency",        contract.getCurrency());
        payload.put("openDate",        contract.getOpenDate().toString());
        payload.put("maturityDate",    contract.getMaturityDate().toString());
        payload.put("sourceAccountNo", contract.getSourceAccountNo());
        payload.put("branchCode",      contract.getBranchCode());
        payload.put("openedBy",        contract.getOpenedBy());
        payload.put("correlationId",   MDC.get("correlationId"));
        payload.put("timestamp",       OffsetDateTime.now().toString());

        publish(Constants.Rabbit.CONTRACT_EXCHANGE,
                Constants.Rabbit.CONTRACT_OPENED_KEY, payload);
    }

    public void publishContractClosed(SavingContract contract,
                                       BigDecimal interestEarned,
                                       BigDecimal totalPayout) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType",       "CONTRACT_CLOSED");
        payload.put("contractNo",      contract.getContractNo());
        payload.put("cif",             contract.getCif());
        payload.put("status",          contract.getStatus());
        payload.put("closeType",       contract.getCloseType());
        payload.put("closedAt",        contract.getClosedAt() != null ? contract.getClosedAt().toString() : null);
        payload.put("principalAmount", contract.getPrincipalAmount());
        payload.put("interestEarned",  interestEarned);
        payload.put("totalPayout",     totalPayout);
        payload.put("currency",        contract.getCurrency());
        payload.put("sourceAccountNo", contract.getSourceAccountNo());
        payload.put("correlationId",   MDC.get("correlationId"));
        payload.put("timestamp",       OffsetDateTime.now().toString());

        publish(Constants.Rabbit.CONTRACT_EXCHANGE,
                Constants.Rabbit.CONTRACT_CLOSED_KEY, payload);
    }

    public void publishContractMatured(SavingContract contract) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType",     "CONTRACT_MATURED");
        payload.put("contractNo",    contract.getContractNo());
        payload.put("cif",           contract.getCif());
        payload.put("maturityDate",  contract.getMaturityDate().toString());
        payload.put("maturedAt",     LocalDate.now().toString());
        payload.put("correlationId", MDC.get("correlationId"));
        payload.put("timestamp",     OffsetDateTime.now().toString());

        publish(Constants.Rabbit.CONTRACT_EXCHANGE,
                Constants.Rabbit.CONTRACT_MATURED_KEY, payload);
    }

    private void publish(String exchange, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("[{}] Published event to {}/{}", MDC.get("correlationId"), exchange, routingKey);
        } catch (AmqpException ex) {
            // Log and swallow — messaging failure must not roll back the DB transaction.
            // The event can be reconciled via outbox or retry later.
            log.error("[{}] Failed to publish event to {}/{}: {}",
                    MDC.get("correlationId"), exchange, routingKey, ex.getMessage(), ex);
        }
    }
}
