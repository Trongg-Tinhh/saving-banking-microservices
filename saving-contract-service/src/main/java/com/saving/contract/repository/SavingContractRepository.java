package com.saving.contract.repository;

import com.saving.contract.entity.SavingContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingContractRepository extends JpaRepository<SavingContract, String> {

    @Query("""
        SELECT c FROM SavingContract c
        LEFT JOIN FETCH c.maturityInstruction
        WHERE c.contractNo = :contractNo
        """)
    Optional<SavingContract> findByContractNoWithInstruction(@Param("contractNo") String contractNo);

    @Query("""
        SELECT c FROM SavingContract c
        WHERE (:cif    IS NULL OR c.cif    = :cif)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.createdAt DESC
        """)
    Page<SavingContract> findByCifAndStatus(
            @Param("cif")    String cif,
            @Param("status") String status,
            Pageable pageable);

    List<SavingContract> findByCifOrderByCreatedAtDesc(String cif);

    Page<SavingContract> findByCif(String cif, Pageable pageable);

    Page<SavingContract> findByStatus(String status, Pageable pageable);

    /** Contracts that have reached maturity date and are still ACTIVE (for lifecycle processing). */
    @Query("""
        SELECT c FROM SavingContract c
        WHERE c.status = 'ACTIVE'
          AND c.maturityDate <= :asOf
        """)
    List<SavingContract> findMaturedContracts(@Param("asOf") LocalDate asOf);

    /** Contracts maturing in the next N days (for pre-maturity notifications). */
    @Query("""
        SELECT c FROM SavingContract c
        WHERE c.status = 'ACTIVE'
          AND c.maturityDate BETWEEN :from AND :to
        """)
    List<SavingContract> findContractsMaturingBetween(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    /** ACTIVE contracts with MONTHLY or QUARTERLY interest payment (not yet matured). */
    @Query("""
        SELECT c FROM SavingContract c
        WHERE c.status = 'ACTIVE'
          AND c.interestPaymentMethod IN ('MONTHLY', 'QUARTERLY')
          AND c.maturityDate > :today
        """)
    Page<SavingContract> findPeriodicInterestContracts(@Param("today") LocalDate today, Pageable pageable);

    // ── Contract number generation ─────────────────────────────────
    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(contract_no, 9) AS INTEGER))
        FROM saving_contract_schema.saving_contracts
        WHERE contract_no LIKE :prefix || '%'
        """, nativeQuery = true)
    Integer findMaxSequenceForYear(@Param("prefix") String prefix);
}
