package com.saving.transaction.listener;

import com.rabbitmq.client.Channel;
import com.saving.transaction.common.Constants;
import com.saving.transaction.dto.request.RecordTransactionRequest;
import com.saving.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to saving.contract.events and records corresponding transactions.
 *
 * CONTRACT_OPENED → DEBIT from source account (principal deposited)
 * CONTRACT_CLOSED → CREDIT to source account (principal + interest returned)
 * CONTRACT_MATURED → informational only, no monetary movement
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class ContractEventListener {

    private final TransactionService transactionService;

    @RabbitListener(queues = Constants.Rabbit.TX_CONTRACT_OPENED_QUEUE, ackMode = "MANUAL")
    public void onContractOpened(Map<String, Object> payload, Message message, Channel channel)
            throws IOException {

        String correlationId = getStr(payload, "correlationId");
        if (correlationId != null) MDC.put("correlationId", correlationId);

        String contractNo  = getStr(payload, "contractNo");
        String txRef       = "CONTRACT-OPEN-" + contractNo;
        log.info("[{}] Received CONTRACT_OPENED event for {}", correlationId, contractNo);

        try {
            RecordTransactionRequest req = new RecordTransactionRequest();
            req.setTransactionRef(txRef);
            req.setAccountNo(getStr(payload, "sourceAccountNo"));
            req.setCif(getStr(payload, "cif"));
            req.setTransactionType(Constants.TxType.DEBIT);
            req.setAmount(parseBigDecimal(payload.get("principalAmount")));
            req.setCurrency(getStr(payload, "currency"));
            req.setDescription("Contract opening deposit — " + contractNo);
            req.setContractNo(contractNo);

            transactionService.recordTransaction(req);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception ex) {
            log.error("[{}] Failed to process CONTRACT_OPENED for {}: {}",
                    correlationId, contractNo, ex.getMessage(), ex);
            // Nack without requeue — goes to DLQ
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @RabbitListener(queues = Constants.Rabbit.TX_CONTRACT_CLOSED_QUEUE, ackMode = "MANUAL")
    public void onContractClosed(Map<String, Object> payload, Message message, Channel channel)
            throws IOException {

        String correlationId = getStr(payload, "correlationId");
        if (correlationId != null) MDC.put("correlationId", correlationId);

        String contractNo = getStr(payload, "contractNo");
        String txRef      = "CONTRACT-CLOSE-" + contractNo;
        log.info("[{}] Received CONTRACT_CLOSED event for {}", correlationId, contractNo);

        try {
            // Credit transaction — total payout (principal + interest)
            RecordTransactionRequest req = new RecordTransactionRequest();
            req.setTransactionRef(txRef);
            req.setAccountNo(getStr(payload, "sourceAccountNo"));
            req.setCif(getStr(payload, "cif"));
            req.setTransactionType(Constants.TxType.CREDIT);
            req.setAmount(parseBigDecimal(payload.get("totalPayout")));
            req.setCurrency(getStr(payload, "currency"));
            req.setDescription("Contract payout — " + contractNo
                    + " (" + getStr(payload, "closeType") + ")");
            req.setContractNo(contractNo);

            transactionService.recordTransaction(req);

            // Also record a separate INTEREST transaction for the interest portion
            BigDecimal interestEarned = parseBigDecimal(payload.get("interestEarned"));
            if (interestEarned != null && interestEarned.compareTo(BigDecimal.ZERO) > 0) {
                RecordTransactionRequest interestReq = new RecordTransactionRequest();
                interestReq.setTransactionRef("CONTRACT-INTEREST-" + contractNo);
                interestReq.setAccountNo(getStr(payload, "sourceAccountNo"));
                interestReq.setCif(getStr(payload, "cif"));
                interestReq.setTransactionType(Constants.TxType.INTEREST);
                interestReq.setAmount(interestEarned);
                interestReq.setCurrency(getStr(payload, "currency"));
                interestReq.setDescription("Interest payout — " + contractNo);
                interestReq.setContractNo(contractNo);
                transactionService.recordTransaction(interestReq);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception ex) {
            log.error("[{}] Failed to process CONTRACT_CLOSED for {}: {}",
                    correlationId, contractNo, ex.getMessage(), ex);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @RabbitListener(queues = Constants.Rabbit.TX_CONTRACT_MATURED_QUEUE, ackMode = "MANUAL")
    public void onContractMatured(Map<String, Object> payload, Message message, Channel channel)
            throws IOException {
        // Informational — no monetary movement at maturity, just log it
        String contractNo    = getStr(payload, "contractNo");
        String correlationId = getStr(payload, "correlationId");
        log.info("[{}] CONTRACT_MATURED received for {} — no transaction recorded",
                correlationId, contractNo);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
