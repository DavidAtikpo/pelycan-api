package com.pelycan.api.repository;

import com.pelycan.api.domain.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    Optional<SessionParticipant> findBySessionIdAndUserIdAndLeftAtIsNull(UUID sessionId, UUID userId);

    List<SessionParticipant> findBySessionIdAndLeftAtIsNull(UUID sessionId);

    long countBySessionIdAndLeftAtIsNull(UUID sessionId);

    Optional<SessionParticipant> findBySessionIdAndAgoraUidAndLeftAtIsNull(UUID sessionId, Long agoraUid);

    @Modifying
    @Query("""
            UPDATE SessionParticipant p SET p.leftAt = :leftAt
            WHERE p.sessionId = :sessionId AND p.leftAt IS NULL
            """)
    int markAllLeft(UUID sessionId, Instant leftAt);
}
