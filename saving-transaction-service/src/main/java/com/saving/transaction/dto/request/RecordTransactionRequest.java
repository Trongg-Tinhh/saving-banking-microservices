package com.saving.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordTransactionRequest {

    /** Idempotency key — required. Duplicate refs are rejected. */
    @NotBlank(message = "Transaction reference is required")
    private String transactionRef;

    @NotBlank(message = "Account number is required")
    private String accountNo;

    private String cif;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEBIT|CREDIT|INTEREST)$", message = "Type must be DEBIT, CREDIT, or INTEREST")
    private String transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "VND";

    private String description;

    /** Optional — links this transaction to a contract. */
    private String contractNo;
}
