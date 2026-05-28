package com.saving.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Internal response returned to Saving Contract Service when opening a contract.
 * Bundles product, term, and the effective interest rate together.
 */
@Data
@Builder
public class ProductRateQueryResponse {

    // Product info
    private String     productCode;
    private String     productName;
    private String     interestPaymentMethod;
    private String     currency;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // Term info
    private String  termId;
    private String  termLabel;
    private Integer termMonths;
    private Integer termDays;

    // Rate info
    private BigDecimal annualRate;
    private LocalDate  rateEffectiveFrom;
    private LocalDate  rateEffectiveTo;

    // Early withdrawal
    private BigDecimal earlyWithdrawalDemandRate;
    private Boolean    earlyWithdrawalUseDemandRate;
    private Integer    earlyWithdrawalMinDaysHeld;
}
