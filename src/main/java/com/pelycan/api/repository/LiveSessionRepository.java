package com.pelycan.api.repository;

import com.pelycan.api.domain.LiveSession;
import com.pelycan.api.domain.LiveSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveSessionRepository extends JpaRepository<LiveSession, UUID> {

    @Query("""
            SELECT s FROM LiveSession s
            LEFT JOIN FETCH s.host
            LEFT JOIN FETCH s.participants p
            WHERE s.status IN :statuses
            ORDER BY s.startTime DESC
            """)
    List<LiveSession> findByStatusesWithDetails(@Param("statuses") List<LiveSessionStatus> statuses);

    @Query("""
            SELECT DISTINCT s FROM LiveSession s
            LEFT JOIN FETCH s.host
            LEFT JOIN FETCH s.participants
            ORDER BY s.startTime DESC
            """)
    List<LiveSession> findAllWithDetails();

    @Query("""
            SELECT s FROM LiveSession s
            LEFT JOIN FETCH s.host
            LEFT JOIN FETCH s.participants
            WHERE s.id = :id
            """)
    Optional<LiveSession> findByIdWithDetails(UUID id);
}
