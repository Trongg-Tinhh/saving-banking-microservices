package com.saving.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Returned after a contract is closed (normal maturity or early withdrawal).
 * Includes computed interest payout so the caller can confirm the credited amount.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloseContractResponse {

    private String     contractNo;
    private String     status;          // CLOSED or EARLY_CLOSED
    private String     closeType;       // MATURITY or EARLY_WITHDRAWAL
    private LocalDate  closedDate;
    private OffsetDateTime closedAt;

    /** The principal that was originally deposited. */
    private BigDecimal principalAmount;

    /** Interest earned (may be prorated for early withdrawal). */
    private BigDecimal interestEarned;

    /** Total credited back to source account (principal + interest). */
    private BigDecimal totalPayout;

    /** Account that received the payout. */
    private String     creditedToAccountNo;

    /** Days the contract was actually held (≤ termDays for early withdrawal). */
    private Long       daysHeld;

    /** Annual interest rate that was locked in at contract open. */
    private BigDecimal interestRate;

    /** True when closed before maturityDate. */
    private Boolean    earlyWithdrawal;
}
