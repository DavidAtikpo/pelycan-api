package com.pelycan.api.repository;

import com.pelycan.api.domain.ProfessionalAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalAppointmentRepository extends JpaRepository<ProfessionalAppointment, UUID> {

    List<ProfessionalAppointment> findByProfessionalIdOrderByAppointmentDateDesc(UUID professionalId);

    List<ProfessionalAppointment> findByUserIdOrderByAppointmentDateDesc(UUID userId);

    Optional<ProfessionalAppointment> findByIdAndProfessionalId(UUID id, UUID professionalId);
}
