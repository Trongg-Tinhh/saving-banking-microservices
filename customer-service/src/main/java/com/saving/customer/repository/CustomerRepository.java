package com.saving.customer.repository;

import com.saving.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    // ── By ID number ───────────────────────────────────────────────
    Optional<Customer> findByIdNumber(String idNumber);

    boolean existsByIdNumber(String idNumber);

    // ── Full customer with KYC and contacts (one query per association) ──
    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN FETCH c.kyc
        WHERE c.cif = :cif
        """)
    Optional<Customer> findByCifWithKyc(@Param("cif") String cif);

    // ── Search with pagination ─────────────────────────────────────
    @Query("""
        SELECT c FROM Customer c
        WHERE (:fullName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
          AND (:idNumber IS NULL OR c.idNumber = :idNumber)
          AND (:status IS NULL OR c.status = :status)
        """)
    Page<Customer> searchCustomers(
            @Param("fullName")  String fullName,
            @Param("idNumber")  String idNumber,
            @Param("status")    String status,
            Pageable pageable);

    // ── Search by contact info (join to contacts table) ─────────────
    @Query("""
        SELECT DISTINCT c FROM Customer c
        JOIN c.contacts ct
        WHERE (:phone IS NULL OR ct.phoneNumber = :phone)
          AND (:email IS NULL OR LOWER(ct.email) = LOWER(:email))
        """)
    Page<Customer> searchByContact(
            @Param("phone") String phone,
            @Param("email") String email,
            Pageable pageable);

    // ── CIF generation: find max CIF number ─────────────────────────
    @Query(value = "SELECT MAX(CAST(SUBSTRING(cif, 4) AS INTEGER)) FROM customer_schema.customers WHERE cif LIKE 'CIF%'",
           nativeQuery = true)
    Integer findMaxCifNumber();
}
