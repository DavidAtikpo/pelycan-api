package com.pelycan.api.repository;

import com.pelycan.api.domain.AlertLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertLocationRepository extends JpaRepository<AlertLocation, UUID> {}
