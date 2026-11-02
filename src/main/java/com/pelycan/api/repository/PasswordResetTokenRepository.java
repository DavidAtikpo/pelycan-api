package com.pelycan.api.repository;

import com.pelycan.api.domain.PasswordResetToken;
import com.pelycan.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.user = :user
              AND t.usedAt IS NULL
              AND t.expiresAt > :now
            ORDER BY t.createdAt DESC
            """)
    List<PasswordResetToken> findActiveTokensForUser(@Param("user") User user, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usedAt = :now
            WHERE t.user = :user AND t.usedAt IS NULL
            """)
    void invalidateActiveTokensForUser(@Param("user") User user, @Param("now") Instant now);
}
