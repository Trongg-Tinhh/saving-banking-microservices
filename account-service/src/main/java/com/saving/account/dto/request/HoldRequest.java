package com.saving.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500)
    private String holdReason;

    /**
     * External reference for hold tracking and later release.
     * Must be unique per active hold (e.g. contract_no, transaction_id).
     */
    @NotBlank(message = "Hold reference is required")
    @Size(max = 100)
    private String holdRef;
}
