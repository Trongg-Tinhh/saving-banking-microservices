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

        // Idempotency: if the same ref already exists, return it
        if (transactionRepository.existsByTransactionRef(request.getTransactionRef())) {
            log.info("[{}] Duplicate transaction ref={} — returning existing record",
                    correlationId, request.getTransactionRef());
            return transactionRepository.findByTransactionRef(request.getTransactionRef())
                    .map(TransactionResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
        }

        // 1. Persist with CBS_PENDING status (outbox step)
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
        log.info("[{}] Transaction recorded: ref={} type={} amount={}",
                correlationId, tx.getTransactionRef(), tx.getTransactionType(), tx.getAmount());

        // 2. Attempt immediate CBS sync (best-effort; scheduler retries failures)
        cbsSyncService.sync(tx);
        transactionRepository.save(tx); // persist CBS sync result

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

        OffsetDateTime from = fromDate != null ? OffsetDateTime.parse(fromDate + "T00:00:00+07:00") : null;
        OffsetDateTime to   = toDate   != null ? OffsetDateTime.parse(toDate   + "T23:59:59+07:00") : null;

        return transactionRepository
                .findAll(TransactionSpec.withFilters(contractNo, cif, txType, status, from, to), pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND,
                        transactionId.toString()));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByRef(String transactionRef) {
        return transactionRepository.findByTransactionRef(transactionRef)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND, transactionRef));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByAccount(String accountNo, Pageable pageable) {
        return transactionRepository
                .findByAccountNoOrderByCreatedAtDesc(accountNo, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByCif(String cif, Pageable pageable) {
        return transactionRepository
                .findByCifOrderByCreatedAtDesc(cif, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getByContract(String contractNo, Pageable pageable) {
        return transactionRepository
                .findByContractNoOrderByCreatedAtDesc(contractNo, pageable)
                .map(TransactionResponse::from);
    }
}
