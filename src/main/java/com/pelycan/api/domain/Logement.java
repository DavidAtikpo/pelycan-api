package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "logements")
@Getter
@Setter
public class Logement {

    @Id
    private UUID id;

    @Column(name = "proprietaire_id", nullable = false)
    private UUID proprietaireId;

    private String titre;
    private String adresse;

    @Column(name = "code_postal")
    private String codePostal;

    private String ville;
    private String type;
    private Integer capacite;
    private Integer surface;
    private Integer prix = 0;
    private String telephone;
    private String email;

    @Column(name = "niveau_securite")
    private String niveauSecurite = "standard";

    private Integer pieces;

    @Column(name = "salles_de_bain")
    private Integer sallesDeBain;

    private Integer etage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> equipements = new HashMap<>();

    private Boolean disponibilite = true;
    private String status = "disponible";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] photos = new String[0];

    @Column(name = "type_hebergement")
    private String typeHebergement = "permanent";

    @Column(name = "date_debut")
    private Instant dateDebut;

    @Column(name = "date_fin")
    private Instant dateFin;

    @Column(name = "conditions_temporaire", columnDefinition = "TEXT")
    private String conditionsTemporaire;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", insertable = false, updatable = false)
    private User proprietaire;

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
