package com.pelycan.api.repository;

import com.pelycan.api.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserNotification> findByUserIdAndReadOrderByCreatedAtDesc(UUID userId, boolean read);

    List<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type);

    long countByUserIdAndReadFalse(UUID userId);

    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserNotification n
            SET n.read = true, n.readAt = :readAt
            WHERE n.userId = :userId AND n.read = false
            """)
    int markAllRead(UUID userId, Instant readAt);
}
