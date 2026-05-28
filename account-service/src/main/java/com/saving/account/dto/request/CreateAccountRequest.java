package com.saving.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "CIF is required")
    @Size(max = 20)
    private String cif;

    @Pattern(regexp = "^(PAYMENT|SAVING|LOAN)$",
             message = "Account type must be PAYMENT, SAVING, or LOAN")
    private String accountType = "PAYMENT";

    @Pattern(regexp = "^(VND|USD|EUR)$", message = "Currency must be VND, USD, or EUR")
    private String currency = "VND";

    @Size(max = 20)
    private String branchCode;

    private LocalDate openDate;
}
