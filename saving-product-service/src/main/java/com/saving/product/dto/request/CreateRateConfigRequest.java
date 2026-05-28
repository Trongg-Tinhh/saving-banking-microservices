package com.saving.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateRateConfigRequest {

    @NotBlank(message = "Term ID is required")
    private String termId;

    @NotNull(message = "Annual rate is required")
    @DecimalMin(value = "0.0", message = "Annual rate must be non-negative")
    private BigDecimal annualRate;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    /** Null means open-ended (current rate). */
    private LocalDate effectiveTo;
}
