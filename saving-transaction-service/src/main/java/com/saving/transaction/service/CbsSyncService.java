package com.saving.transaction.service;

import com.saving.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Synchronises transactions to the Core Banking System (CBS).
 * The CBS mock runs at cbs.url (core-banking-mock service).
 *
 * On success, sets cbsSyncStatus = SYNCED and records the CBS reference.
 * On failure, increments cbsSyncAttempts and records the error.
 * A background scheduler retries PENDING/FAILED entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CbsSyncService {

    private final RestTemplate restTemplate;

    @Value("${cbs.url}")
    private String cbsUrl;

    /**
     * Push a single transaction to CBS.
     * Mutates cbsSyncStatus, cbsReference, cbsSyncAttempts on the passed entity.
     * The caller is responsible for saving to DB afterwards.
     *
     * @return true if CBS accepted the transaction
     */
    public boolean sync(Transaction tx) {
        tx.setCbsSyncAttempts(tx.getCbsSyncAttempts() + 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionRef",  tx.getTransactionRef());
        payload.put("accountNo",       tx.getAccountNo());
        payload.put("cif",             tx.getCif());
        payload.put("transactionType", tx.getTransactionType());
        payload.put("amount",          tx.getAmount());
        payload.put("currency",        tx.getCurrency());
        payload.put("description",     tx.getDescription());
        payload.put("contractNo",      tx.getContractNo());
        payload.put("correlationId",   tx.getCorrelationId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Propagate correlation ID if present in MDC
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set("X-Correlation-ID", correlationId);
        }

        try {
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    cbsUrl + "/api/transactions",
                    new HttpEntity<>(payload, headers),
                    Map.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Object ref = responseEntity.getBody().get("cbsReference");
                tx.setCbsReference(ref != null ? ref.toString() : null);
                tx.setCbsSyncStatus("SYNCED");
                tx.setCbsSyncedAt(OffsetDateTime.now());
                tx.setCbsSyncError(null);
                log.info("CBS sync success for txRef={} cbsRef={}", tx.getTransactionRef(), ref);
                return true;
            } else {
                String err = "CBS returned status: " + responseEntity.getStatusCode();
                tx.setCbsSyncStatus("FAILED");
                tx.setCbsSyncError(err);
                log.warn("CBS sync failed for txRef={}: {}", tx.getTransactionRef(), err);
                return false;
            }

        } catch (RestClientException ex) {
            String err = ex.getMessage();
            tx.setCbsSyncStatus("FAILED");
            tx.setCbsSyncError(err != null && err.length() > 900 ? err.substring(0, 900) : err);
            log.warn("CBS sync error for txRef={}: {}", tx.getTransactionRef(), err);
            return false;
        }
    }
}
