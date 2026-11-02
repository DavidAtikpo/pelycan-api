package com.pelycan.api.repository;

import com.pelycan.api.domain.DemandeAjoutLogement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemandeAjoutLogementRepository extends JpaRepository<DemandeAjoutLogement, UUID> {

    @Query("""
            SELECT d FROM DemandeAjoutLogement d
            WHERE d.userId = :userId AND d.statut IN ('en_attente', 'approuvee')
            """)
    List<DemandeAjoutLogement> findActiveByUserId(UUID userId);

    List<DemandeAjoutLogement> findAllByOrderByDateCreationDesc();
}
