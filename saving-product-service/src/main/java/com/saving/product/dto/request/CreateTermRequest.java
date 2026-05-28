package com.saving.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTermRequest {

    @NotBlank(message = "Term ID is required")
    @Size(max = 50)
    private String termId;

    @Min(value = 1, message = "Term months must be at least 1")
    private Integer termMonths;

    @Min(value = 1, message = "Term days must be at least 1")
    private Integer termDays;

    @NotBlank(message = "Term label is required")
    @Size(max = 50)
    private String termLabel;
}
