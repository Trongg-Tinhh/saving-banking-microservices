package com.saving.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    @Size(max = 200)
    private String productName;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal minAmount;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal maxAmount;

    private Boolean isActive;

    private String description;
}
