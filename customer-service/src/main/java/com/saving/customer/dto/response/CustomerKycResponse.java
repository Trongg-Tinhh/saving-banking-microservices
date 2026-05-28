package com.saving.customer.dto.response;

import com.saving.customer.entity.CustomerKyc;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerKycResponse {

    private UUID           kycId;
    private String         cif;
    private String         kycStatus;
    private OffsetDateTime verifiedAt;
    private String         verifiedBy;
    private String         rejectionReason;
    private String         docType;
    private String         docUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CustomerKycResponse from(CustomerKyc k) {
        return CustomerKycResponse.builder()
                .kycId(k.getKycId())
                .cif(k.getCustomer() != null ? k.getCustomer().getCif() : null)
                .kycStatus(k.getKycStatus())
                .verifiedAt(k.getVerifiedAt())
                .verifiedBy(k.getVerifiedBy())
                .rejectionReason(k.getRejectionReason())
                .docType(k.getDocType())
                .docUrl(k.getDocUrl())
                .createdAt(k.getCreatedAt())
                .updatedAt(k.getUpdatedAt())
                .build();
    }
}
