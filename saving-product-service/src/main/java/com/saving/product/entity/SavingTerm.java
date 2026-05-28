package com.saving.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "saving_terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingTerm {

    @Id
    @Column(name = "term_id", length = 50, nullable = false)
    private String termId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", nullable = false)
    private SavingProduct product;

    /** Number of months for this term (e.g. 1, 3, 6, 12, 24). */
    @Column(name = "term_months")
    private Integer termMonths;

    /** Exact number of days (e.g. 30, 91, 182, 365, 730). */
    @Column(name = "term_days")
    private Integer termDays;

    @Column(name = "term_label", length = 50, nullable = false)
    private String termLabel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
