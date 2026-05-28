package com.saving.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "account_no", length = 20, nullable = false)
    private String accountNo;

    @Column(name = "cif", length = 20, nullable = false)
    private String cif;

    @Column(name = "account_type", length = 30, nullable = false)
    @Builder.Default
    private String accountType = "PAYMENT";

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "open_date", nullable = false)
    @Builder.Default
    private LocalDate openDate = LocalDate.now();

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ── Association to balance (1:1) ──────────────────────────────
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AccountBalance balance;
}
