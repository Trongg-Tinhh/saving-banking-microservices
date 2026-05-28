package com.saving.customer.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Internal response for other microservices to validate a customer's CIF and KYC status.
 * Called by: saving-contract-service, account-service.
 */
@Data
@Builder
public class CustomerValidationResponse {

    private String  cif;
    private String  fullName;
    private String  status;         // ACTIVE | INACTIVE | BLOCKED
    private String  kycStatus;      // NOT_VERIFIED | PENDING | VERIFIED | REJECTED
    private boolean valid;          // cif exists && status == ACTIVE && kyc == VERIFIED
    private String  invalidReason;

    public static CustomerValidationResponse valid(String cif, String fullName, String kycStatus) {
        return CustomerValidationResponse.builder()
                .cif(cif)
                .fullName(fullName)
                .status("ACTIVE")
                .kycStatus(kycStatus)
                .valid(true)
                .build();
    }

    public static CustomerValidationResponse invalid(String cif, String reason) {
        return CustomerValidationResponse.builder()
                .cif(cif)
                .valid(false)
                .invalidReason(reason)
                .build();
    }
}
