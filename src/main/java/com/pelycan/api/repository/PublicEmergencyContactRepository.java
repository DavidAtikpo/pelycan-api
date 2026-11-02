package com.pelycan.api.repository;

import com.pelycan.api.domain.PublicEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PublicEmergencyContactRepository extends JpaRepository<PublicEmergencyContact, UUID> {

    List<PublicEmergencyContact> findByActiveTrueOrderByOrderIndexAscNameAsc();
}
