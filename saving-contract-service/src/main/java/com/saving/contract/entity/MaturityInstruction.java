package com.saving.contract.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maturity_instructions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaturityInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instruction_id", updatable = false, nullable = false)
    private UUID instructionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_no", nullable = false, unique = true)
    private SavingContract contract;

    /**
     * What to do at maturity:
     * - TRANSFER_PRINCIPAL_AND_INTEREST → credit all to receiving_account_no
     * - RENEW_PRINCIPAL → renew contract with same principal, transfer interest
     * - RENEW_PRINCIPAL_AND_INTEREST → renew with principal + interest accumulated
     */
    @Column(name = "instruction_type", length = 50, nullable = false)
    private String instructionType;

    /** New term for renewal (only if instruction is RENEW_*). */
    @Column(name = "new_term_id", length = 50)
    private String newTermId;

    /** Account to receive principal/interest at maturity. */
    @Column(name = "receiving_account_no", length = 20)
    private String receivingAccountNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
