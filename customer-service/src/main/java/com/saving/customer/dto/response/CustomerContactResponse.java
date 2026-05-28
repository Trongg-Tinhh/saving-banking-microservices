package com.saving.customer.dto.response;

import com.saving.customer.entity.CustomerContact;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerContactResponse {

    private UUID   contactId;
    private String phoneNumber;
    private String email;
    private String address;
    private String district;
    private String city;
    private Boolean isPrimary;
    private OffsetDateTime createdAt;

    public static CustomerContactResponse from(CustomerContact c) {
        return CustomerContactResponse.builder()
                .contactId(c.getContactId())
                .phoneNumber(c.getPhoneNumber())
                .email(c.getEmail())
                .address(c.getAddress())
                .district(c.getDistrict())
                .city(c.getCity())
                .isPrimary(c.getIsPrimary())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
