package com.saving.auth.repository;

import com.saving.auth.entity.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    Optional<LoginSession> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT s FROM LoginSession s WHERE s.user.userId = :userId AND s.isRevoked = false AND s.expiresAt > :now")
    java.util.List<LoginSession> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE LoginSession s SET s.isRevoked = true, s.revokedAt = :now WHERE s.user.userId = :userId AND s.isRevoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE LoginSession s SET s.isRevoked = true, s.revokedAt = :now WHERE s.sessionId = :sessionId")
    int revokeById(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM LoginSession s WHERE s.expiresAt < :cutoff")
    int deleteExpiredSessions(@Param("cutoff") Instant cutoff);
}
