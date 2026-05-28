package com.saving.account.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Result of a debit, credit, hold, or release-hold operation.
 */
@Data
@Builder
public class BalanceOperationResponse {

    private String          accountNo;
    private String          operation;          // DEBIT | CREDIT | HOLD | RELEASE_HOLD
    private BigDecimal      amount;
    private BigDecimal      availableBalance;   // After operation
    private BigDecimal      ledgerBalance;      // After operation
    private BigDecimal      holdAmount;         // After operation
    private String          reference;          // Idempotency key / hold ref
    private OffsetDateTime  processedAt;

    public static BalanceOperationResponse of(String accountNo, String operation,
                                              BigDecimal amount,  String reference,
                                              AccountBalanceResponse balance) {
        return BalanceOperationResponse.builder()
                .accountNo(accountNo)
                .operation(operation)
                .amount(amount)
                .reference(reference)
                .availableBalance(balance.getAvailableBalance())
                .ledgerBalance(balance.getLedgerBalance())
                .holdAmount(balance.getHoldAmount())
                .processedAt(OffsetDateTime.now())
                .build();
    }
}
