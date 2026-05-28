package com.saving.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "early_withdrawal_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarlyWithdrawalPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false)
    private UUID policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", nullable = false)
    private SavingProduct product;

    /** Minimum days the contract must be held before early withdrawal is allowed. */
    @Column(name = "min_days_held", nullable = false)
    @Builder.Default
    private Integer minDaysHeld = 0;

    /** Penalty rate applied on top of demand rate (% per year). */
    @Column(name = "penalty_rate", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    /** If true, apply demand rate instead of contracted rate for early withdrawal interest. */
    @Column(name = "use_demand_rate", nullable = false)
    @Builder.Default
    private Boolean useDemandRate = true;

    /** The demand (non-term) savings rate applied on early withdrawal (% per year). */
    @Column(name = "demand_rate", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal demandRate = new BigDecimal("0.5");

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
