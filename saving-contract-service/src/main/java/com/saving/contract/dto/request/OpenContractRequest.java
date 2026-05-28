package com.saving.contract.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OpenContractRequest {

    @NotBlank(message = "CIF is required")
    private String cif;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Term ID is required")
    private String termId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000", message = "Principal amount must be at least 1,000 VND")
    private BigDecimal principalAmount;

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNo;

    /** Open date — defaults to today if not provided. */
    private LocalDate openDate;

    @Size(max = 20)
    private String branchCode;

    /** Maturity instruction — what to do when contract matures. */
    private MaturityInstructionDto maturityInstruction;

    @Data
    public static class MaturityInstructionDto {

        @NotBlank
        @Pattern(regexp = "^(TRANSFER_PRINCIPAL_AND_INTEREST|RENEW_PRINCIPAL|RENEW_PRINCIPAL_AND_INTEREST)$",
                 message = "Invalid instruction type")
        private String instructionType;

        /** Required for RENEW_* instruction types. */
        private String newTermId;

        /** Account to receive funds at maturity. Defaults to sourceAccountNo if null. */
        private String receivingAccountNo;
    }
}
