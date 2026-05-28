package com.saving.contract.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Maps to AccountValidationResponse from account-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountValidationResult {
    private String     accountNo;
    private String     cif;
    private String     accountType;
    private String     status;
    private String     currency;
    private BigDecimal availableBalance;
    private boolean    valid;
    private String     invalidReason;
}
