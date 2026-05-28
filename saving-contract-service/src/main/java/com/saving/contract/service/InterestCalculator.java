package com.saving.contract.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless interest calculation utilities.
 *
 * Formula (all methods):
 *   Simple Interest = principal × (annualRate / 100) × (days / 365)
 *
 * Interest payment methods:
 *   END_OF_TERM  — full term days used; paid out only at maturity
 *   MONTHLY      — 30 days per period (informational; payout still at close)
 *   QUARTERLY    — 91 days per period (informational; payout still at close)
 *
 * Early withdrawal:
 *   Uses the demand rate (penalty/alternative rate) and only the days held,
 *   not the contracted term days.
 */
@Component
public class InterestCalculator {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int        SCALE        = 6;

    /**
     * Calculate the interest earned for a contract held to full term.
     *
     * @param principal   the deposited amount
     * @param annualRate  the locked-in annual interest rate (e.g. 6.5 means 6.5 %)
     * @param termDays    total duration of the contract in days
     * @return interest amount rounded to 6 decimal places
     */
    public BigDecimal calculateFullTermInterest(BigDecimal principal,
                                                BigDecimal annualRate,
                                                int termDays) {
        // interest = principal * (rate / 100) * (termDays / 365)
        return principal
                .multiply(annualRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(termDays).divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the interest earned for an early withdrawal.
     * Uses the demand (penalty) rate and only the number of days actually held.
     *
     * @param principal  the deposited amount
     * @param demandRate the early-withdrawal annual rate (e.g. 0.5 means 0.5 %)
     * @param daysHeld   number of days the contract was actually held
     * @return prorated interest amount rounded to 2 decimal places
     */
    public BigDecimal calculateEarlyWithdrawalInterest(BigDecimal principal,
                                                        BigDecimal demandRate,
                                                        long daysHeld) {
        return principal
                .multiply(demandRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(daysHeld).divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convenience: total payout = principal + interest.
     */
    public BigDecimal totalPayout(BigDecimal principal, BigDecimal interest) {
        return principal.add(interest).setScale(2, RoundingMode.HALF_UP);
    }
}
