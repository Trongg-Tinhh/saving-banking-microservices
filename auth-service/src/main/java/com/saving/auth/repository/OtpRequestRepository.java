package com.saving.auth.repository;

import com.saving.auth.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {

    @Query("""
        SELECT o FROM OtpRequest o
        WHERE o.user.userId = :userId
          AND o.purpose = :purpose
          AND o.isUsed = false
          AND o.expiresAt > :now
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<OtpRequest> findLatestValidOtp(
            @Param("userId") UUID userId,
            @Param("purpose") String purpose,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE OtpRequest o SET o.isUsed = true WHERE o.otpId = :otpId")
    void markAsUsed(@Param("otpId") UUID otpId);

    @Modifying
    @Query("DELETE FROM OtpRequest o WHERE o.expiresAt < :cutoff")
    int deleteExpiredOtps(@Param("cutoff") Instant cutoff);
}
