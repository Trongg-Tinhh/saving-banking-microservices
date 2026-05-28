package com.saving.product.dto.response;

import com.saving.product.entity.InterestRateConfig;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class InterestRateResponse {

    private UUID       rateId;
    private String     productCode;
    private String     termId;
    private String     termLabel;
    private BigDecimal annualRate;
    private LocalDate  effectiveFrom;
    private LocalDate  effectiveTo;
    private Boolean    isActive;

    public static InterestRateResponse from(InterestRateConfig r) {
        return InterestRateResponse.builder()
                .rateId(r.getRateId())
                .productCode(r.getProduct() != null ? r.getProduct().getProductCode() : null)
                .termId(r.getTerm() != null ? r.getTerm().getTermId() : null)
                .termLabel(r.getTerm() != null ? r.getTerm().getTermLabel() : null)
                .annualRate(r.getAnnualRate())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .isActive(r.getIsActive())
                .build();
    }
}
