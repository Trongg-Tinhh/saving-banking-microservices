package com.saving.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_hold_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHoldLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hold_id", updatable = false, nullable = false)
    private UUID holdId;

    @Column(name = "account_no", length = 20, nullable = false)
    private String accountNo;

    @Column(name = "hold_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal holdAmount;

    @Column(name = "hold_reason", columnDefinition = "TEXT")
    private String holdReason;

    /**
     * External reference — e.g. contract_no, idempotency key.
     * Used to find and release a specific hold.
     */
    @Column(name = "hold_ref", length = 100)
    private String holdRef;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;
}
