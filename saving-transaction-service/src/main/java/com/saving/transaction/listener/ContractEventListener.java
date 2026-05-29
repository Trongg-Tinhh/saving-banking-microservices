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

/**
 * Listens to saving.contract.events and records corresponding transactions.
 *
 * CONTRACT_OPENED  → DEBIT from source account (principal deposited)
 * CONTRACT_CLOSED  → CREDIT + INTEREST to source account (payout)
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

        String contractNo = getStr(payload, "contractNo");
        String cif        = getStr(payload, "cif");
        Object amount     = payload.get("principalAmount");
        String txRef      = "CONTRACT-OPEN-" + contractNo;

        log.info("[CONTRACT_OPENED_EVENT] received: contractNo={} cif={} principal={} ref={}",
                contractNo, cif, amount, txRef);
        try {
            RecordTransactionRequest req = new RecordTransactionRequest();
            req.setTransactionRef(txRef);
            req.setAccountNo(getStr(payload, "sourceAccountNo"));
            req.setCif(cif);
            req.setTransactionType(Constants.TxType.DEBIT);
            req.setAmount(parseBigDecimal(amount));
            req.setCurrency(getStr(payload, "currency"));
            req.setDescription("Contract opening deposit — " + contractNo);
            req.setContractNo(contractNo);

            transactionService.recordTransaction(req);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("[CONTRACT_OPENED_EVENT] SUCCESS: contractNo={} DEBIT recorded", contractNo);

        } catch (Exception ex) {
            log.error("[CONTRACT_OPENED_EVENT] FAILED: contractNo={} reason={}", contractNo, ex.getMessage(), ex);
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

        String contractNo    = getStr(payload, "contractNo");
        String cif           = getStr(payload, "cif");
        Object totalPayout   = payload.get("totalPayout");
        Object interestEarned = payload.get("interestEarned");
        String closeType     = getStr(payload, "closeType");
        String txRef         = "CONTRACT-CLOSE-" + contractNo;

        log.info("[CONTRACT_CLOSED_EVENT] received: contractNo={} cif={} payout={} interest={} type={} ref={}",
                contractNo, cif, totalPayout, interestEarned, closeType, txRef);
        try {
            // Credit transaction — total payout (principal + interest)
            RecordTransactionRequest req = new RecordTransactionRequest();
            req.setTransactionRef(txRef);
            req.setAccountNo(getStr(payload, "sourceAccountNo"));
            req.setCif(cif);
            req.setTransactionType(Constants.TxType.CREDIT);
            req.setAmount(parseBigDecimal(totalPayout));
            req.setCurrency(getStr(payload, "currency"));
            req.setDescription("Contract payout — " + contractNo + " (" + closeType + ")");
            req.setContractNo(contractNo);
            transactionService.recordTransaction(req);

            // Separate INTEREST transaction for the interest portion
            BigDecimal interest = parseBigDecimal(interestEarned);
            if (interest != null && interest.compareTo(BigDecimal.ZERO) > 0) {
                String interestRef = "CONTRACT-INTEREST-" + contractNo;
                log.info("[CONTRACT_CLOSED_EVENT] recording INTEREST transaction: ref={} amount={}",
                        interestRef, interest);
                RecordTransactionRequest interestReq = new RecordTransactionRequest();
                interestReq.setTransactionRef(interestRef);
                interestReq.setAccountNo(getStr(payload, "sourceAccountNo"));
                interestReq.setCif(cif);
                interestReq.setTransactionType(Constants.TxType.INTEREST);
                interestReq.setAmount(interest);
                interestReq.setCurrency(getStr(payload, "currency"));
                interestReq.setDescription("Interest payout — " + contractNo);
                interestReq.setContractNo(contractNo);
                transactionService.recordTransaction(interestReq);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("[CONTRACT_CLOSED_EVENT] SUCCESS: contractNo={} CREDIT+INTEREST recorded", contractNo);

        } catch (Exception ex) {
            log.error("[CONTRACT_CLOSED_EVENT] FAILED: contractNo={} reason={}", contractNo, ex.getMessage(), ex);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @RabbitListener(queues = Constants.Rabbit.TX_CONTRACT_MATURED_QUEUE, ackMode = "MANUAL")
    public void onContractMatured(Map<String, Object> payload, Message message, Channel channel)
            throws IOException {
        String contractNo    = getStr(payload, "contractNo");
        String correlationId = getStr(payload, "correlationId");
        if (correlationId != null) MDC.put("correlationId", correlationId);
        log.info("[CONTRACT_MATURED_EVENT] received: contractNo={} — no monetary movement", contractNo);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        MDC.remove("correlationId");
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
