package com.pelycan.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pro_case_notes")
@Getter
@Setter
public class ProCaseNote {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false, length = 100)
    private String caseId;

    @Column(name = "case_type", nullable = false, length = 50)
    private String caseType;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
