package com.saving.customer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.customer.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {

    private String         cif;
    private String         fullName;
    private LocalDate      dateOfBirth;
    private String         gender;
    private String         nationality;
    private String         idNumber;
    private String         idType;
    private String         status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Embedded KYC summary
    private String         kycStatus;

    // Contacts (loaded on-demand)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CustomerContactResponse> contacts;

    // ── Factory ───────────────────────────────────────────────────

    public static CustomerResponse from(Customer c) {
        CustomerResponseBuilder builder = CustomerResponse.builder()
                .cif(c.getCif())
                .fullName(c.getFullName())
                .dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender())
                .nationality(c.getNationality())
                .idNumber(c.getIdNumber())
                .idType(c.getIdType())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt());

        if (c.getKyc() != null) {
            builder.kycStatus(c.getKyc().getKycStatus());
        }

        return builder.build();
    }

    public static CustomerResponse fromWithContacts(Customer c) {
        CustomerResponse response = from(c);
        if (c.getContacts() != null && !c.getContacts().isEmpty()) {
            response.setContacts(c.getContacts().stream()
                    .map(CustomerContactResponse::from)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
