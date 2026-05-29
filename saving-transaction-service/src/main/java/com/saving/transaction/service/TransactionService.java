package com.saving.transaction.service;

import com.saving.transaction.common.Constants;
import com.saving.transaction.dto.request.RecordTransactionRequest;
import com.saving.transaction.dto.response.TransactionResponse;
import com.saving.transaction.entity.Transaction;
import com.saving.transaction.exception.BusinessException;
import com.saving.transaction.exception.ErrorCode;
import com.saving.transaction.repository.TransactionRepository;
import com.saving.transaction.repository.TransactionSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CbsSyncService        cbsSyncService;

    // ────────────────────────────────────────────────────────────────────────
    // Record (outbox pattern: save first, then sync to CBS)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse recordTransaction(RecordTransactionRequest request) {
        String correlationId = MDC.get("correlationId");

        // ── Idempotency check ─────────────────────────────────────────────────
        log.info("[RECORD_TX] Step 1/3 — idempotency check: ref={} type={} amount={} account={}",
                request.getTransactionRef(), request.getTransactionType(),
                request.getAmount(), request.getAccountNo());

        if (transactionRepository.existsByTransactionRef(request.getTransactionRef())) {
            log.info("[RECORD_TX] IDEMPOTENT — ref already exists, returning existing: ref={}",
                    request.getTransactionRef());
            return transactionRepository.findByTransactionRef(request.getTransactionRef())
                    .map(TransactionResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
        }

        // ── 2. Persist (COMPLETED / CBS_PENDING) ─────────────────────────────
        log.info("[RECORD_TX] Step 2/3 — persist transaction: ref={} contract={}",
                request.getTransactionRef(), request.getContractNo());

        Transaction tx = Transaction.builder()
                .transactionRef(request.getTransactionRef())
                .accountNo(request.getAccountNo())
                .cif(request.getCif())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .description(request.getDescription())
                .contractNo(request.getContractNo())
                .status(Constants.TxStatus.COMPLETED)
                .cbsSyncStatus(Constants.CbsStatus.PENDING)
                .correlationId(correlationId)
                .build();

        transactionRepository.save(tx);

        // ── 3. CBS sync (best-effort; scheduler retries failures) ─────────────
        log.info("[RECORD_TX] Step 3/3 — CBS sync attempt: ref={}", tx.getTransactionRef());
        cbsSyncService.sync(tx);
        transactionRepository.save(tx);

        log.info("[RECORD_TX] SUCCESS — ref={} type={} amount={} {} cbsStatus={}",
                tx.getTransactionRef(), tx.getTransactionType(),
                tx.getAmount(), tx.getCurrency(), tx.getCbsSyncStatus());

        return TransactionResponse.from(tx);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Queries
    // ────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(
            String contractNo,
            String cif,
            String txType,
            String status,
            String fromDate,
            String toDate,
            Pageable pageable) {

        log.info("[LIST_TX] query: contractNo={} cif={} type={} status={} from={} to={} page={}/size={}",
                contractNo, cif, txType, status, fromDate, toDate,
                pageable.getPageNumber(), pageable.getPageSize());

        OffsetDateTime from = fromDate != null ? OffsetDateTime.parse(fromDate + "T00:00:00+07:00") : null;
        OffsetDateTime to   = toDate   != null ? OffsetDateTime.parse(toDate   + "T23:59:59+07:00") : null;

        Page<TransactionResponse> result = transactionRepository
                .findAll(TransactionSpec.withFilters(contractNo, cif, txType, status, from, to), pageable)
                .map(TransactionResponse::from);

        log.info("[LIST_TX] OK: total={}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        log.info("[GET_TX] lookup by id: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .map(tx -> {
                    log.info("[GET_TX] OK: id={} ref={} type={} status={}",
                            transactionId, tx.getTransactionRef(),
                            tx.getTransactionType(), tx.getStatus());
                    return TransactionResponse.from(tx);
                })
                .orElseThrow(() -> {
                    log.warn("[GET_TX] NOT FOUND: id={}", transactionId);
                    return new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, transactionId.toString());
                });
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByRef(String transactionRef) {
        log.info("[GET_TX_BY_REF] lookup: ref={}", transactionRef);
        return transactionRepository.findByTransactionRef(transactionRef)
                .map(tx -> {
                    log.info("[GET_TX_BY_REF] OK: ref={} type={} amount={} cbsStatus={}",
                            transactionRef, tx.getTransactionType(),
                            tx.getAmount(), tx.getCbsSyncStatus());
                    return TransactionResponse.from(tx);
                })
                .orElseThrow(() -> {
                    log.warn("[GET_TX_BY_REF] NOT FOUND: ref={}", transactionRef);
                    return new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, transactionRef);
                });
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByAccount(String accountNo, Pageable pageable) {
        log.info("[GET_TX_BY_ACCOUNT] accountNo={} page={}/size={}",
                accountNo, pageable.getPageNumber(), pageable.getPageSize());
        Page<TransactionResponse> result = transactionRepository
                .findByAccountNoOrderByCreatedAtDesc(accountNo, pageable)
                .map(TransactionResponse::from);
        log.info("[GET_TX_BY_ACCOUNT] OK: accountNo={} total={}", accountNo, result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByCif(String cif, Pageable pageable) {
        log.info("[GET_TX_BY_CIF] cif={} page={}/size={}",
                cif, pageable.getPageNumber(), pageable.getPageSize());
        Page<TransactionResponse> result = transactionRepository
                .findByCifOrderByCreatedAtDesc(cif, pageable)
                .map(TransactionResponse::from);
        log.info("[GET_TX_BY_CIF] OK: cif={} total={}", cif, result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByContract(String contractNo, Pageable pageable) {
        log.info("[GET_TX_BY_CONTRACT] contractNo={} page={}/size={}",
                contractNo, pageable.getPageNumber(), pageable.getPageSize());
        Page<TransactionResponse> result = transactionRepository
                .findByContractNoOrderByCreatedAtDesc(contractNo, pageable)
                .map(TransactionResponse::from);
        log.info("[GET_TX_BY_CONTRACT] OK: contractNo={} total={}", contractNo, result.getTotalElements());
        return result;
    }
}
