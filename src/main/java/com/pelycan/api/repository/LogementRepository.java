package com.pelycan.api.repository;

import com.pelycan.api.domain.Logement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LogementRepository extends JpaRepository<Logement, UUID> {

    List<Logement> findByStatusOrderByCreatedAtDesc(String status);

    List<Logement> findByProprietaireIdOrderByCreatedAtDesc(UUID proprietaireId);

    @Query("""
            SELECT l FROM Logement l
            LEFT JOIN FETCH l.proprietaire
            ORDER BY l.createdAt DESC
            """)
    List<Logement> findAllWithProprietaire();
}
