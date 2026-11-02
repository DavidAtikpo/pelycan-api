package com.pelycan.api.repository;

import com.pelycan.api.domain.DangerAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DangerAlertRepository extends JpaRepository<DangerAlert, UUID> {

    List<DangerAlert> findAllByOrderByAlertTimestampDesc();

    Optional<DangerAlert> findFirstByStatusOrderByAlertTimestampDesc(String status);
}
