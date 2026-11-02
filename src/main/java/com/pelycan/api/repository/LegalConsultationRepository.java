package com.pelycan.api.repository;

import com.pelycan.api.domain.LegalConsultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalConsultationRepository extends JpaRepository<LegalConsultation, UUID> {

    List<LegalConsultation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<LegalConsultation> findByIdAndUserId(UUID id, UUID userId);

    List<LegalConsultation> findByAssignedToOrderByCreatedAtDesc(UUID assignedTo);

    List<LegalConsultation> findByAssignedToIsNullOrderByCreatedAtDesc();

    List<LegalConsultation> findByAssignedToIsNotNullOrderByCreatedAtDesc();

    long countByAssignedToIsNull();

    long countByAssignedToIsNotNull();

    long countByUrgenceTrue();

    long countByStatusIgnoreCase(String status);
}
