package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demandes")
@Getter
@Setter
public class Demande {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String email;

    @Column(name = "nombre_personnes", nullable = false, columnDefinition = "TEXT")
    private String nombrePersonnes;

    @Column(name = "niveau_urgence", nullable = false)
    private String niveauUrgence = "normal";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "logement_id")
    private UUID logementId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "centre_type")
    private String centreType;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status = "en_attente";

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logement_id", insertable = false, updatable = false)
    private Logement logement;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
