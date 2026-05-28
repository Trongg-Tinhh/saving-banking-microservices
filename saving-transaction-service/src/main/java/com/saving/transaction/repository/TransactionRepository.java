package com.saving.transaction.repository;

import com.saving.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    boolean existsByTransactionRef(String transactionRef);

    Page<Transaction> findByAccountNoOrderByCreatedAtDesc(String accountNo, Pageable pageable);

    Page<Transaction> findByCifOrderByCreatedAtDesc(String cif, Pageable pageable);

    Page<Transaction> findByContractNoOrderByCreatedAtDesc(String contractNo, Pageable pageable);

    /** Find transactions that have not yet been successfully pushed to CBS. */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.cbsSyncStatus IN ('PENDING', 'FAILED')
          AND t.cbsSyncAttempts < :maxAttempts
        ORDER BY t.createdAt ASC
        """)
    List<Transaction> findPendingCbsSync(@Param("maxAttempts") int maxAttempts, Pageable pageable);
}
