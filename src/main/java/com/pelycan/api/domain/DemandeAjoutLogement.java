package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demandes_ajout_logement")
@Getter
@Setter
public class DemandeAjoutLogement {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(nullable = false, length = 100)
    private String email;

    private String justificatif;

    @Column(nullable = false)
    private String statut = "en_attente";

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_mise_a_jour")
    private Instant dateMiseAJour;

    @Column(name = "raison_demande", nullable = false, columnDefinition = "TEXT")
    private String raisonDemande;

    @Column(name = "raison_refus", columnDefinition = "TEXT")
    private String raisonRefus;

    @Column(name = "est_proprio", nullable = false)
    private boolean estProprio;

    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bulletins_salaire", columnDefinition = "text[]")
    private List<String> bulletinsSalaire = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "contrat_travail", columnDefinition = "text[]")
    private List<String> contratTravail = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "quittances_loyer", columnDefinition = "text[]")
    private List<String> quittancesLoyer = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "justificatif_prise_en_charge", columnDefinition = "text[]")
    private List<String> justificatifPriseEnCharge = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "piece_identite", columnDefinition = "text[]")
    private List<String> pieceIdentite = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "livret_famille", columnDefinition = "text[]")
    private List<String> livretFamille = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "notification_caf", columnDefinition = "text[]")
    private List<String> notificationCaf = new ArrayList<>();

    @Column(name = "numero_piece_identite")
    private String numeroPieceIdentite;

    @Column(name = "numero_demande_logement")
    private String numeroDemandeLogement;

    @Column(name = "numero_dalo")
    private String numeroDalo;

    @Column(name = "nombre_personnes", length = 50)
    private String nombrePersonnes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (dateCreation == null) {
            dateCreation = now;
        }
        createdAt = now;
        updatedAt = now;
        dateMiseAJour = now;
    }

    @PreUpdate
    void preUpdate() {
        Instant now = Instant.now();
        updatedAt = now;
        dateMiseAJour = now;
    }
}
