package com.saving.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateKycStatusRequest {

    @NotBlank(message = "KYC status is required")
    @Pattern(regexp = "^(NOT_VERIFIED|PENDING|VERIFIED|REJECTED)$",
             message = "KYC status must be one of: NOT_VERIFIED, PENDING, VERIFIED, REJECTED")
    private String kycStatus;

    private String rejectionReason;

    // Verified by (staff username) — set by server from JWT if not provided
    private String verifiedBy;

    private String docType;
    private String docUrl;
}
