package com.saving.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.contract.entity.SavingContract;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractResponse {

    private String     contractNo;
    private String     cif;
    private String     productCode;
    private String     termId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private String     currency;
    private LocalDate  openDate;
    private LocalDate  maturityDate;
    private String     status;
    private String     interestPaymentMethod;
    private String     sourceAccountNo;
    private String     branchCode;
    private String     openedBy;
    private OffsetDateTime closedAt;
    private String     closeType;
    private Long       version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** Calculated fields */
    private Long       daysRemaining;
    private Long       daysHeld;

    /** Embedded maturity instruction */
    private MaturityInstructionResponse maturityInstruction;

    public static ContractResponse from(SavingContract c) {
        long daysRemaining = Math.max(0,
                LocalDate.now().until(c.getMaturityDate(), java.time.temporal.ChronoUnit.DAYS));
        long daysHeld = c.getOpenDate().until(
                LocalDate.now().isAfter(c.getMaturityDate()) ? c.getMaturityDate() : LocalDate.now(),
                java.time.temporal.ChronoUnit.DAYS);

        ContractResponseBuilder builder = ContractResponse.builder()
                .contractNo(c.getContractNo())
                .cif(c.getCif())
                .productCode(c.getProductCode())
                .termId(c.getTermId())
                .principalAmount(c.getPrincipalAmount())
                .interestRate(c.getInterestRate())
                .currency(c.getCurrency())
                .openDate(c.getOpenDate())
                .maturityDate(c.getMaturityDate())
                .status(c.getStatus())
                .interestPaymentMethod(c.getInterestPaymentMethod())
                .sourceAccountNo(c.getSourceAccountNo())
                .branchCode(c.getBranchCode())
                .openedBy(c.getOpenedBy())
                .closedAt(c.getClosedAt())
                .closeType(c.getCloseType())
                .version(c.getVersion())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .daysRemaining(daysRemaining)
                .daysHeld(daysHeld);

        if (c.getMaturityInstruction() != null) {
            builder.maturityInstruction(MaturityInstructionResponse.from(c.getMaturityInstruction()));
        }

        return builder.build();
    }
}
