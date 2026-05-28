package com.saving.customer.repository;

import com.saving.customer.entity.CustomerKyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerKycRepository extends JpaRepository<CustomerKyc, UUID> {

    Optional<CustomerKyc> findByCustomer_Cif(String cif);

    boolean existsByCustomer_Cif(String cif);

    @Modifying
    @Query("""
        UPDATE CustomerKyc k
        SET k.kycStatus       = :status,
            k.verifiedAt      = :verifiedAt,
            k.verifiedBy      = :verifiedBy,
            k.rejectionReason = :rejectionReason,
            k.updatedAt       = CURRENT_TIMESTAMP
        WHERE k.customer.cif  = :cif
        """)
    int updateKycStatus(@Param("cif")             String cif,
                        @Param("status")          String status,
                        @Param("verifiedAt")      OffsetDateTime verifiedAt,
                        @Param("verifiedBy")      String verifiedBy,
                        @Param("rejectionReason") String rejectionReason);
}
