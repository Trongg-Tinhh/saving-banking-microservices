package com.saving.account.dto.response;

import com.saving.account.entity.Account;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight account DTO for list results.
 */
@Data
@Builder
public class AccountSummaryResponse {

    private String     accountNo;
    private String     cif;
    private String     accountType;
    private String     currency;
    private String     status;
    private LocalDate  openDate;
    private BigDecimal availableBalance;
    private BigDecimal ledgerBalance;

    public static AccountSummaryResponse from(Account a) {
        AccountSummaryResponseBuilder builder = AccountSummaryResponse.builder()
                .accountNo(a.getAccountNo())
                .cif(a.getCif())
                .accountType(a.getAccountType())
                .currency(a.getCurrency())
                .status(a.getStatus())
                .openDate(a.getOpenDate());

        if (a.getBalance() != null) {
            builder.availableBalance(a.getBalance().getAvailableBalance());
            builder.ledgerBalance(a.getBalance().getLedgerBalance());
        }

        return builder.build();
    }
}
