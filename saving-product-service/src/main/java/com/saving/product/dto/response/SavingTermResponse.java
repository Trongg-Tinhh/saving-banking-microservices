package com.saving.product.dto.response;

import com.saving.product.entity.SavingTerm;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SavingTermResponse {

    private String  termId;
    private String  productCode;
    private Integer termMonths;
    private Integer termDays;
    private String  termLabel;
    private Boolean isActive;

    /** Current annual rate (effective today). Populated by service layer. */
    private BigDecimal annualRate;

    public static SavingTermResponse from(SavingTerm t) {
        return SavingTermResponse.builder()
                .termId(t.getTermId())
                .productCode(t.getProduct() != null ? t.getProduct().getProductCode() : null)
                .termMonths(t.getTermMonths())
                .termDays(t.getTermDays())
                .termLabel(t.getTermLabel())
                .isActive(t.getIsActive())
                .build();
    }
}
