package com.saving.product.repository;

import com.saving.product.entity.SavingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingProductRepository extends JpaRepository<SavingProduct, String> {

    List<SavingProduct> findByIsActiveTrueOrderByProductCodeAsc();

    List<SavingProduct> findAllByOrderByProductCodeAsc();

    @Query("""
        SELECT p FROM SavingProduct p
        LEFT JOIN FETCH p.terms t
        WHERE p.productCode = :productCode
          AND (t IS NULL OR t.isActive = true)
        """)
    Optional<SavingProduct> findByProductCodeWithActiveTerms(@Param("productCode") String productCode);

    @Query("""
        SELECT p FROM SavingProduct p
        LEFT JOIN FETCH p.terms
        LEFT JOIN FETCH p.earlyWithdrawalPolicy
        WHERE p.productCode = :productCode
        """)
    Optional<SavingProduct> findByProductCodeWithAll(@Param("productCode") String productCode);

    boolean existsByProductCode(String productCode);
}
