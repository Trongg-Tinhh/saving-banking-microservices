package com.saving.transaction.scheduler;

import com.saving.transaction.entity.Transaction;
import com.saving.transaction.repository.TransactionRepository;
import com.saving.transaction.service.CbsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox retry scheduler — polls for transactions that failed CBS sync
 * and retries them up to MAX_ATTEMPTS times.
 *
 * Runs every 60 seconds. Processes at most 50 records per run to
 * avoid holding a long transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CbsSyncScheduler {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE   = 50;

    private final TransactionRepository transactionRepository;
    private final CbsSyncService        cbsSyncService;

    @Scheduled(fixedDelayString = "${cbs.sync-retry-delay-ms:60000}")
    @Transactional
    public void retryCbsSync() {
        List<Transaction> pending = transactionRepository.findPendingCbsSync(
                MAX_ATTEMPTS, PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        log.info("CBS sync retry: processing {} pending transactions", pending.size());
        int success = 0;
        int failure = 0;

        for (Transaction tx : pending) {
            boolean synced = cbsSyncService.sync(tx);
            transactionRepository.save(tx);
            if (synced) success++; else failure++;
        }

        log.info("CBS sync retry complete: success={} failure={}", success, failure);
    }
}
