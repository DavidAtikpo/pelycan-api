package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "groupe_parole_sessions")
@Getter
@Setter
public class GroupeParoleSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "session_type", nullable = false)
    private String sessionType = "visio";

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "session_time", nullable = false)
    private String sessionTime = "00:00";

    @Column(nullable = false)
    private String location = "En ligne";

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 15;

    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    private String status = "ouvert";

    @Column(name = "animateur_name")
    private String animateurName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "created_by")
    private UUID createdBy;

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
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
