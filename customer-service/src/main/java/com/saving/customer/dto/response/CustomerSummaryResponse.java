package com.saving.customer.dto.response;

import com.saving.customer.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Lightweight customer DTO for list/search results.
 * Does NOT include contacts or KYC details.
 */
@Data
@Builder
public class CustomerSummaryResponse {

    private String    cif;
    private String    fullName;
    private LocalDate dateOfBirth;
    private String    gender;
    private String    idNumber;
    private String    idType;
    private String    status;
    private String    kycStatus;

    public static CustomerSummaryResponse from(Customer c) {
        return CustomerSummaryResponse.builder()
                .cif(c.getCif())
                .fullName(c.getFullName())
                .dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender())
                .idNumber(c.getIdNumber())
                .idType(c.getIdType())
                .status(c.getStatus())
                .kycStatus(c.getKyc() != null ? c.getKyc().getKycStatus() : "NOT_VERIFIED")
                .build();
    }
}
