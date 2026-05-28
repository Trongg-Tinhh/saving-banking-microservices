package com.saving.contract.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps to CustomerValidationResponse from customer-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerValidationResult {
    private String  cif;
    private String  fullName;
    private String  status;
    private String  kycStatus;
    private boolean valid;
    private String  invalidReason;
}
