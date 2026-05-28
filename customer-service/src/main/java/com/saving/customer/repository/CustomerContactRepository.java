package com.saving.customer.repository;

import com.saving.customer.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, UUID> {

    List<CustomerContact> findByCustomer_CifOrderByIsPrimaryDescCreatedAtAsc(String cif);

    Optional<CustomerContact> findByCustomer_CifAndIsPrimaryTrue(String cif);

    boolean existsByCustomer_CifAndIsPrimaryTrue(String cif);

    @Modifying
    @Query("""
        UPDATE CustomerContact cc
        SET cc.isPrimary = false
        WHERE cc.customer.cif = :cif
          AND cc.isPrimary = true
        """)
    int unsetPrimaryContacts(@Param("cif") String cif);

    long countByCustomer_Cif(String cif);
}
