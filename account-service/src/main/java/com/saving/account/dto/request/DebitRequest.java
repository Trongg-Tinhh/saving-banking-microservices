package com.saving.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255)
    private String description;

    /**
     * Idempotency key — callers must provide a unique reference.
     * Prevents duplicate debits if the request is retried.
     */
    @NotBlank(message = "Reference (idempotency key) is required")
    @Size(max = 100)
    private String reference;

    /**
     * If true, debit against held funds (debitFromHold).
     * Used by Saving Contract Service when contract is funded from a pre-placed hold.
     */
    private boolean useHold = false;

    private String currency = "VND";
}
