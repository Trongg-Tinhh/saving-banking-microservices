package com.saving.account.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.account.entity.Account;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    private String     accountNo;
    private String     cif;
    private String     accountType;
    private String     currency;
    private String     status;
    private LocalDate  openDate;
    private String     branchCode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Embedded balance (included when fetched with balance)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AccountBalanceResponse balance;

    public static AccountResponse from(Account a) {
        AccountResponseBuilder builder = AccountResponse.builder()
                .accountNo(a.getAccountNo())
                .cif(a.getCif())
                .accountType(a.getAccountType())
                .currency(a.getCurrency())
                .status(a.getStatus())
                .openDate(a.getOpenDate())
                .branchCode(a.getBranchCode())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt());

        if (a.getBalance() != null) {
            builder.balance(AccountBalanceResponse.from(a.getBalance()));
        }

        return builder.build();
    }
}
