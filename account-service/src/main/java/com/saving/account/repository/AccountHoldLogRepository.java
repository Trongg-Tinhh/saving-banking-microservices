package com.saving.account.repository;

import com.saving.account.entity.AccountHoldLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountHoldLogRepository extends JpaRepository<AccountHoldLog, UUID> {

    List<AccountHoldLog> findByAccountNoAndStatusOrderByCreatedAtDesc(String accountNo, String status);

    Optional<AccountHoldLog> findByHoldRefAndStatus(String holdRef, String status);

    Optional<AccountHoldLog> findByHoldRef(String holdRef);

    @Modifying
    @Query("""
        UPDATE AccountHoldLog h
        SET h.status = 'RELEASED', h.releasedAt = CURRENT_TIMESTAMP
        WHERE h.holdRef = :holdRef AND h.status = 'ACTIVE'
        """)
    int releaseByHoldRef(@Param("holdRef") String holdRef);

    @Modifying
    @Query("""
        UPDATE AccountHoldLog h
        SET h.status = 'CANCELLED', h.releasedAt = CURRENT_TIMESTAMP
        WHERE h.holdRef = :holdRef AND h.status = 'ACTIVE'
        """)
    int cancelByHoldRef(@Param("holdRef") String holdRef);

    boolean existsByHoldRefAndStatus(String holdRef, String status);
}
