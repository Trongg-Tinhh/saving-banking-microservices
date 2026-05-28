package com.saving.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.transaction.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private UUID          transactionId;
    private String        transactionRef;
    private String        accountNo;
    private String        cif;
    private String        transactionType;
    private BigDecimal    amount;
    private String        currency;
    private String        description;
    private String        contractNo;
    private String        cbsReference;
    private String        status;
    private String        cbsSyncStatus;
    private String        correlationId;
    private OffsetDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .transactionRef(t.getTransactionRef())
                .accountNo(t.getAccountNo())
                .cif(t.getCif())
                .transactionType(t.getTransactionType())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .contractNo(t.getContractNo())
                .cbsReference(t.getCbsReference())
                .status(t.getStatus())
                .cbsSyncStatus(t.getCbsSyncStatus())
                .correlationId(t.getCorrelationId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
