package com.saving.account.repository;

import com.saving.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    // ── By CIF ────────────────────────────────────────────────────
    List<Account> findByCifOrderByCreatedAtAsc(String cif);

    List<Account> findByCifAndStatusOrderByCreatedAtAsc(String cif, String status);

    // ── With balance (avoid N+1) ───────────────────────────────────
    @Query("""
        SELECT a FROM Account a
        LEFT JOIN FETCH a.balance
        WHERE a.accountNo = :accountNo
        """)
    Optional<Account> findByAccountNoWithBalance(@Param("accountNo") String accountNo);

    @Query("""
        SELECT a FROM Account a
        LEFT JOIN FETCH a.balance
        WHERE a.cif = :cif
        """)
    List<Account> findByCifWithBalance(@Param("cif") String cif);

    // ── Account number generation: max sequence for a CIF ─────────
    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(account_no, 7) AS INTEGER))
        FROM account_schema.accounts
        WHERE account_no LIKE :prefix || '%'
        """, nativeQuery = true)
    Integer findMaxAccountSequence(@Param("prefix") String prefix);

    // ── Existence checks ──────────────────────────────────────────
    boolean existsByAccountNoAndCif(String accountNo, String cif);

    long countByCif(String cif);
}
