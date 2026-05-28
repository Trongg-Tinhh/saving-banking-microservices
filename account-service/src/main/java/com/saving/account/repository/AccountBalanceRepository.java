package com.saving.account.repository;

import com.saving.account.entity.AccountBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {

    Optional<AccountBalance> findByAccount_AccountNo(String accountNo);

    /**
     * Fetch balance with PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
     * Used for all balance-modifying operations to prevent lost updates.
     * Must be called within a @Transactional context.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AccountBalance b WHERE b.account.accountNo = :accountNo")
    Optional<AccountBalance> findByAccountNoForUpdate(@Param("accountNo") String accountNo);
}
