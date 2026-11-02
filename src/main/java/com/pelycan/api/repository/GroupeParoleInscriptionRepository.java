package com.pelycan.api.repository;

import com.pelycan.api.domain.GroupeParoleInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupeParoleInscriptionRepository extends JpaRepository<GroupeParoleInscription, UUID> {

    boolean existsBySessionIdAndUserId(UUID sessionId, UUID userId);

    Optional<GroupeParoleInscription> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    long countBySessionId(UUID sessionId);

    @Query("SELECT i.sessionId FROM GroupeParoleInscription i WHERE i.userId = :userId")
    List<UUID> findSessionIdsByUserId(@Param("userId") UUID userId);
}
