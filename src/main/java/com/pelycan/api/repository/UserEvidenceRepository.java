package com.pelycan.api.repository;

import com.pelycan.api.domain.UserEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserEvidenceRepository extends JpaRepository<UserEvidence, UUID> {

    List<UserEvidence> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserEvidence> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type);

    Optional<UserEvidence> findByIdAndUserId(UUID id, UUID userId);
}
