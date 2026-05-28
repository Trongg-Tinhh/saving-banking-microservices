package com.saving.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_kyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kyc_id", updatable = false, nullable = false)
    private UUID kycId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cif", nullable = false)
    private Customer customer;

    @Column(name = "kyc_status", length = 20, nullable = false)
    @Builder.Default
    private String kycStatus = "NOT_VERIFIED";

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "doc_type", length = 50)
    private String docType;

    @Column(name = "doc_url", columnDefinition = "TEXT")
    private String docUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────

    public boolean isVerified() {
        return "VERIFIED".equals(this.kycStatus);
    }

    public boolean isPending() {
        return "PENDING".equals(this.kycStatus);
    }
}
