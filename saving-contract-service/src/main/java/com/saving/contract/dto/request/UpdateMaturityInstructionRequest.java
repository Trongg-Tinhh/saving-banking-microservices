package com.saving.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMaturityInstructionRequest {

    @NotBlank
    @Pattern(regexp = "^(TRANSFER_PRINCIPAL_AND_INTEREST|RENEW_PRINCIPAL|RENEW_PRINCIPAL_AND_INTEREST)$",
             message = "Invalid instruction type")
    private String instructionType;

    private String newTermId;
    private String receivingAccountNo;
}
