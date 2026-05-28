package com.saving.account.dto.response;

import com.saving.account.entity.AccountBalance;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AccountBalanceResponse {

    private UUID       balanceId;
    private String     accountNo;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal holdAmount;
    private String     currency;
    private Long       version;
    private OffsetDateTime updatedAt;

    public static AccountBalanceResponse from(AccountBalance b) {
        return AccountBalanceResponse.builder()
                .balanceId(b.getBalanceId())
                .accountNo(b.getAccount() != null ? b.getAccount().getAccountNo() : null)
                .availableBalance(b.getAvailableBalance())
                .ledgerBalance(b.getLedgerBalance())
                .holdAmount(b.getHoldAmount())
                .currency(b.getCurrency())
                .version(b.getVersion())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
