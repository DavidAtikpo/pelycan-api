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
@Table(name = "consultation_requests")
@Getter
@Setter
public class ConsultationRequest {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "preferred_date", length = 50)
    private String preferredDate;

    @Column(nullable = false, length = 50)
    private String status = "pending";

    @Column(nullable = false, length = 50)
    private String priority = "medium";

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
