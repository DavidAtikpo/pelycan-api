package com.pelycan.api.repository;

import com.pelycan.api.domain.ProfessionalAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, UUID> {

    List<ProfessionalAvailability> findByProfessionalIdAndAvailabilityTypeOrderByCreatedAtDesc(
            UUID professionalId, String availabilityType);

    Optional<ProfessionalAvailability> findByIdAndProfessionalId(UUID id, UUID professionalId);
}
