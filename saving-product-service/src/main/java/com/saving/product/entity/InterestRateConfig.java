package com.saving.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "interest_rate_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rate_product_term_from",
        columnNames = {"product_code", "term_id", "effective_from"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rate_id", updatable = false, nullable = false)
    private UUID rateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", nullable = false)
    private SavingProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private SavingTerm term;

    /** Annual interest rate in percent (e.g. 5.5 = 5.5% per year). */
    @Column(name = "annual_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal annualRate;

    /** Rate is effective FROM this date (inclusive). */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Rate is effective TO this date (inclusive). NULL means open-ended (current). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Helper ────────────────────────────────────────────────────

    /**
     * Check if this rate is effective on the given date.
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive)) return false;
        if (date.isBefore(effectiveFrom)) return false;
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}
