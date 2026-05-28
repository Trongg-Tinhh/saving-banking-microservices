package com.saving.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertEarlyWithdrawalPolicyRequest {

    /** Minimum number of days the contract must be held before early withdrawal is allowed. */
    @NotNull(message = "minDaysHeld is required")
    @Min(value = 0, message = "minDaysHeld must be >= 0")
    private Integer minDaysHeld;

    /** Penalty interest rate applied on early withdrawal (% per annum). */
    @NotNull(message = "penaltyRate is required")
    @DecimalMin(value = "0.0", message = "penaltyRate must be >= 0")
    private BigDecimal penaltyRate;

    /** If true, apply demand-deposit rate instead of contracted rate when calculating early interest. */
    @NotNull(message = "useDemandRate is required")
    private Boolean useDemandRate;

    /** Demand-deposit rate used when useDemandRate = true (% per annum). */
    @NotNull(message = "demandRate is required")
    @DecimalMin(value = "0.0", message = "demandRate must be >= 0")
    private BigDecimal demandRate;
}
