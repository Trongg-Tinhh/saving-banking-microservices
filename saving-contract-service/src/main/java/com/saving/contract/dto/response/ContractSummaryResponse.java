package com.saving.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saving.contract.entity.SavingContract;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lightweight summary returned in list queries (no embedded sub-objects).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractSummaryResponse {

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
    private Long       daysRemaining;

    public static ContractSummaryResponse from(SavingContract c) {
        long daysRemaining = Math.max(0,
                LocalDate.now().until(c.getMaturityDate(), ChronoUnit.DAYS));

        return ContractSummaryResponse.builder()
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
                .daysRemaining(daysRemaining)
                .build();
    }
}
