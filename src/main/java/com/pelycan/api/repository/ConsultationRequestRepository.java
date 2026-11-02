package com.pelycan.api.repository;

import com.pelycan.api.domain.ConsultationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, UUID> {

    List<ConsultationRequest> findByProfessionalIdOrderByCreatedAtDesc(UUID professionalId);

    Optional<ConsultationRequest> findByIdAndProfessionalId(UUID id, UUID professionalId);
}
