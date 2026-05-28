package com.saving.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.product.entity.SavingProduct;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavingProductResponse {

    private String     productCode;
    private String     productName;
    private String     currency;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String     interestPaymentMethod;
    private Boolean    isActive;
    private String     description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Embedded collections (populated when fetching full detail)
    private List<SavingTermResponse>         terms;
    private List<InterestRateResponse>       currentRates;
    private EarlyWithdrawalPolicyResponse    earlyWithdrawalPolicy;

    public static SavingProductResponse from(SavingProduct p) {
        return SavingProductResponse.builder()
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .currency(p.getCurrency())
                .minAmount(p.getMinAmount())
                .maxAmount(p.getMaxAmount())
                .interestPaymentMethod(p.getInterestPaymentMethod())
                .isActive(p.getIsActive())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
