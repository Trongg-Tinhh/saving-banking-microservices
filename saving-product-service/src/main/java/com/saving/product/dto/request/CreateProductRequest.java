package com.saving.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Product code must be uppercase letters, digits, or underscores")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    private String productName;

    @Pattern(regexp = "^(VND|USD|EUR)$", message = "Currency must be VND, USD, or EUR")
    private String currency = "VND";

    @DecimalMin(value = "0", inclusive = false, message = "Min amount must be positive")
    private BigDecimal minAmount;

    @DecimalMin(value = "0", inclusive = false, message = "Max amount must be positive")
    private BigDecimal maxAmount;

    @NotBlank(message = "Interest payment method is required")
    @Pattern(regexp = "^(END_OF_TERM|MONTHLY|QUARTERLY|UPFRONT)$",
             message = "Invalid interest payment method")
    private String interestPaymentMethod;

    private String description;
}
