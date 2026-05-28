package com.saving.product.repository;

import com.saving.product.entity.EarlyWithdrawalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EarlyWithdrawalPolicyRepository extends JpaRepository<EarlyWithdrawalPolicy, UUID> {

    Optional<EarlyWithdrawalPolicy> findByProduct_ProductCode(String productCode);

    boolean existsByProduct_ProductCode(String productCode);
}
