package com.saving.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saving_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingProduct {

    @Id
    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "min_amount", precision = 20, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 20, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "interest_payment_method", length = 30, nullable = false)
    @Builder.Default
    private String interestPaymentMethod = "END_OF_TERM";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Associations ──────────────────────────────────────────────

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SavingTerm> terms = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InterestRateConfig> rateConfigs = new ArrayList<>();

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EarlyWithdrawalPolicy earlyWithdrawalPolicy;
}
