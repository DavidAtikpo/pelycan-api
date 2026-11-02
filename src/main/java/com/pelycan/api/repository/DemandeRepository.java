package com.pelycan.api.repository;

import com.pelycan.api.domain.Demande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, UUID> {

    List<Demande> findByLogementIdOrderByCreatedAtDesc(UUID logementId);

    List<Demande> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
