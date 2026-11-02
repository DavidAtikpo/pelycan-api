package com.pelycan.api.repository;

import com.pelycan.api.domain.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, UUID> {

    @Query("""
            SELECT t FROM UserFcmToken t
            WHERE t.userId IN (
                SELECT u.id FROM User u WHERE u.role = com.pelycan.api.domain.UserRole.admin
            )
            """)
    List<UserFcmToken> findAllAdminTokens();

    Optional<UserFcmToken> findByUserIdAndToken(UUID userId, String token);

    Optional<UserFcmToken> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
