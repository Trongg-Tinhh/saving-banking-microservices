package com.saving.product.repository;

import com.saving.product.entity.InterestRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterestRateConfigRepository extends JpaRepository<InterestRateConfig, UUID> {

    List<InterestRateConfig> findByProduct_ProductCodeOrderByEffectiveFromDesc(String productCode);

    List<InterestRateConfig> findByProduct_ProductCodeAndTerm_TermIdOrderByEffectiveFromDesc(
            String productCode, String termId);

    /**
     * Find the rate that is effective on a given date for a specific product+term.
     * Picks the most recently started rate whose window covers the target date.
     *
     * effective_from <= :date  AND  (effective_to IS NULL OR effective_to >= :date)
     */
    @Query("""
        SELECT r FROM InterestRateConfig r
        WHERE r.product.productCode = :productCode
          AND r.term.termId         = :termId
          AND r.isActive            = true
          AND r.effectiveFrom      <= :date
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
        ORDER BY r.effectiveFrom DESC
        """)
    List<InterestRateConfig> findEffectiveRates(
            @Param("productCode") String productCode,
            @Param("termId")      String termId,
            @Param("date")        LocalDate date);

    /** Convenience: get first (latest) effective rate. */
    default Optional<InterestRateConfig> findEffectiveRate(String productCode, String termId, LocalDate date) {
        List<InterestRateConfig> rates = findEffectiveRates(productCode, termId, date);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
    }

    /** Check if a rate with the same (product, term, effective_from) already exists. */
    boolean existsByProduct_ProductCodeAndTerm_TermIdAndEffectiveFrom(
            String productCode, String termId, LocalDate effectiveFrom);

    /**
     * Find all current rates (effective_to IS NULL) for a product — useful for listing.
     */
    @Query("""
        SELECT r FROM InterestRateConfig r
        JOIN FETCH r.term t
        WHERE r.product.productCode = :productCode
          AND r.isActive  = true
          AND r.effectiveTo IS NULL
        ORDER BY t.termMonths ASC
        """)
    List<InterestRateConfig> findCurrentRates(@Param("productCode") String productCode);
}
