package com.saving.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255)
    private String description;

    /**
     * Idempotency key — ensures credit is applied only once.
     */
    @NotBlank(message = "Reference (idempotency key) is required")
    @Size(max = 100)
    private String reference;

    private String currency = "VND";
}
