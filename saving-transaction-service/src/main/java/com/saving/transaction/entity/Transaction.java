package com.saving.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Central ledger entry for every monetary movement in the system.
 *
 * Each transaction originates from an event (contract opened/closed)
 * or a direct API call, and is independently synced to the Core Banking
 * System (CBS) for reconciliation.
 */
@Entity
@Table(name = "transactions",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_transactions_ref",
               columnNames = "transaction_ref"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    /**
     * Idempotency key — callers supply this so duplicate submissions are detected.
     * Typically: "CONTRACT-OPEN-{contractNo}" or "CONTRACT-CLOSE-{contractNo}".
     */
    @Column(name = "transaction_ref", length = 100, nullable = false)
    private String transactionRef;

    @Column(name = "account_no", length = 20, nullable = false)
    private String accountNo;

    @Column(name = "cif", length = 20)
    private String cif;

    /** DEBIT | CREDIT | INTEREST */
    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "description", length = 500)
    private String description;

    /** Contract that triggered this transaction (nullable for non-contract movements). */
    @Column(name = "contract_no", length = 50)
    private String contractNo;

    /** External reference returned by CBS. */
    @Column(name = "cbs_reference", length = 100)
    private String cbsReference;

    /** COMPLETED | FAILED */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    /** PENDING | SYNCED | FAILED — tracks CBS synchronisation. */
    @Column(name = "cbs_sync_status", length = 20, nullable = false)
    @Builder.Default
    private String cbsSyncStatus = "PENDING";

    @Column(name = "cbs_sync_attempts", nullable = false)
    @Builder.Default
    private int cbsSyncAttempts = 0;

    @Column(name = "cbs_sync_error", length = 1000)
    private String cbsSyncError;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "cbs_synced_at")
    private OffsetDateTime cbsSyncedAt;
}
