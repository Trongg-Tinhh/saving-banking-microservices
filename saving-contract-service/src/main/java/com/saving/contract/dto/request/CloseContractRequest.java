package com.saving.contract.dto.request;

import lombok.Data;

@Data
public class CloseContractRequest {

    /**
     * Account to receive principal + interest.
     * If null, defaults to maturity instruction's receiving_account_no
     * or the original source_account_no.
     */
    private String receivingAccountNo;

    /** Optional reason for early withdrawal. */
    private String reason;
}
