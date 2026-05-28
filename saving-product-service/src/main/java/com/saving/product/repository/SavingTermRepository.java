package com.saving.product.repository;

import com.saving.product.entity.SavingTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingTermRepository extends JpaRepository<SavingTerm, String> {

    List<SavingTerm> findByProduct_ProductCodeAndIsActiveTrueOrderByTermMonthsAsc(String productCode);

    List<SavingTerm> findByProduct_ProductCodeOrderByTermMonthsAsc(String productCode);

    Optional<SavingTerm> findByTermIdAndProduct_ProductCode(String termId, String productCode);

    boolean existsByTermIdAndProduct_ProductCode(String termId, String productCode);
}
