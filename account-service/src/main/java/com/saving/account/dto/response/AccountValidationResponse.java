package com.saving.account.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal validation response for other microservices.
 * Called by: saving-contract-service (to verify source account before contract creation).
 */
@Data
@Builder
public class AccountValidationResponse {

    private String     accountNo;
    private String     cif;
    private String     accountType;
    private String     status;
    private String     currency;
    private BigDecimal availableBalance;
    private boolean    valid;
    private String     invalidReason;

    public static AccountValidationResponse valid(String accountNo, String cif,
                                                  String accountType, String currency,
                                                  BigDecimal availableBalance) {
        return AccountValidationResponse.builder()
                .accountNo(accountNo)
                .cif(cif)
                .accountType(accountType)
                .currency(currency)
                .availableBalance(availableBalance)
                .status("ACTIVE")
                .valid(true)
                .build();
    }

    public static AccountValidationResponse invalid(String accountNo, String reason) {
        return AccountValidationResponse.builder()
                .accountNo(accountNo)
                .valid(false)
                .invalidReason(reason)
                .build();
    }
}
