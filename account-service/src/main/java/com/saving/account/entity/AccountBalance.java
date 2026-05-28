package com.saving.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "balance_id", updatable = false, nullable = false)
    private UUID balanceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_no", nullable = false, unique = true)
    private Account account;

    /**
     * Available balance = funds the customer can actually use.
     * Formula: ledger_balance - hold_amount
     * DB CHECK constraint: available_balance >= 0
     */
    @Column(name = "available_balance", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Ledger balance = accounting balance (actual funds in account).
     */
    @Column(name = "ledger_balance", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    /**
     * Hold amount = funds reserved but not yet debited.
     * DB CHECK constraint: hold_amount >= 0
     */
    @Column(name = "hold_amount", nullable = false, precision = 20, scale = 2)
    @Builder.Default
    private BigDecimal holdAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Optimistic locking version — Hibernate auto-increments on each UPDATE.
     * DB also has version BIGINT for idempotency checks at SQL level.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // ── Business helpers ──────────────────────────────────────────

    /**
     * Check if available balance is sufficient for the requested amount.
     */
    public boolean hasSufficientFunds(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Debit: reduce both available and ledger balance.
     * Pre-condition: hasSufficientFunds(amount) == true
     */
    public void debit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.ledgerBalance    = this.ledgerBalance.subtract(amount);
    }

    /**
     * Credit: increase both available and ledger balance.
     */
    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        this.ledgerBalance    = this.ledgerBalance.add(amount);
    }

    /**
     * Place hold: reserve funds (reduces available, increases hold).
     * Pre-condition: hasSufficientFunds(amount) == true
     */
    public void placeHold(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.holdAmount       = this.holdAmount.add(amount);
    }

    /**
     * Release hold: unreserve funds (increases available, reduces hold).
     */
    public void releaseHold(BigDecimal amount) {
        this.holdAmount       = this.holdAmount.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    /**
     * Debit from hold: funds already reserved — reduce ledger and hold.
     * Used when a held debit finally settles.
     * Pre-condition: holdAmount >= amount
     */
    public void debitFromHold(BigDecimal amount) {
        this.holdAmount    = this.holdAmount.subtract(amount);
        this.ledgerBalance = this.ledgerBalance.subtract(amount);
        // availableBalance unchanged (was already reduced at hold time)
    }
}
