package com.saving.contract.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps to ProductRateQueryResponse from saving-product-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRateResult {
    private String     productCode;
    private String     productName;
    private String     interestPaymentMethod;
    private String     currency;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String     termId;
    private String     termLabel;
    private Integer    termMonths;
    private Integer    termDays;
    private BigDecimal annualRate;
    private LocalDate  rateEffectiveFrom;
    private LocalDate  rateEffectiveTo;
    private BigDecimal earlyWithdrawalDemandRate;
    private Boolean    earlyWithdrawalUseDemandRate;
    private Integer    earlyWithdrawalMinDaysHeld;
}
