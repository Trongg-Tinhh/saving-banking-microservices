package com.saving.contract.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "saving_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingContract {

    @Id
    @Column(name = "contract_no", length = 50, nullable = false)
    private String contractNo;

    @Column(name = "cif", length = 20, nullable = false)
    private String cif;

    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Column(name = "term_id", length = 50, nullable = false)
    private String termId;

    @Column(name = "principal_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal principalAmount;

    /** Interest rate locked at contract open (% per year, e.g. 5.5). */
    @Column(name = "interest_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "interest_payment_method", length = 30, nullable = false)
    private String interestPaymentMethod;

    @Column(name = "source_account_no", length = 20, nullable = false)
    private String sourceAccountNo;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "opened_by", length = 100)
    private String openedBy;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "close_type", length = 30)
    private String closeType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    // ── Association ───────────────────────────────────────────────
    @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MaturityInstruction maturityInstruction;

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isActive()   { return "ACTIVE".equals(status); }
    public boolean isMatured()  { return "MATURED".equals(status); }
    public boolean isClosed()   { return "CLOSED".equals(status) || "EARLY_CLOSED".equals(status); }

    public boolean isEarlyWithdrawal() {
        return LocalDate.now().isBefore(maturityDate);
    }

    public long getDaysHeld() {
        LocalDate closeDate = LocalDate.now();
        return openDate.until(closeDate, java.time.temporal.ChronoUnit.DAYS);
    }
}
