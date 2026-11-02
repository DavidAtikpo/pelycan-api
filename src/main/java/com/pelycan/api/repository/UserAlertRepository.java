package com.pelycan.api.repository;

import com.pelycan.api.domain.UserAlert;
import com.pelycan.api.domain.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAlertRepository extends JpaRepository<UserAlert, UUID> {

    Optional<UserAlert> findFirstByUserIdAndStatus(UUID userId, AlertStatus status);

    List<UserAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserAlert> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, AlertStatus status);

    @Query("""
            SELECT a FROM UserAlert a
            LEFT JOIN FETCH a.user
            WHERE a.status = :status
            ORDER BY a.createdAt DESC
            """)
    List<UserAlert> findByStatusWithUser(AlertStatus status);

    @Query("""
            SELECT a FROM UserAlert a
            LEFT JOIN FETCH a.user
            ORDER BY a.createdAt DESC
            """)
    List<UserAlert> findAllWithUser();

    long countByUserIdAndStatus(UUID userId, AlertStatus status);
}
