package com.pelycan.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_consultations")
@Getter
@Setter
public class LegalConsultation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String status = "en_attente";

    @Column(nullable = false, length = 50)
    private String modalite;

    @Column(nullable = false)
    private boolean urgence;

    @Column(name = "date_consultation")
    private Instant dateConsultation;

    @Column(name = "avocat_nom")
    private String avocatNom;

    @Column(name = "avocat_email")
    private String avocatEmail;

    @Column(name = "avocat_telephone", length = 50)
    private String avocatTelephone;

    @Column(name = "avocat_specialite")
    private String avocatSpecialite;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "notes_internes", columnDefinition = "TEXT")
    private String notesInternes;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
