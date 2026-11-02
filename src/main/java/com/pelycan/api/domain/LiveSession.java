package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "live_sessions")
@Getter
@Setter
public class LiveSession {

    @Id
    private UUID id;

    @Column(name = "groupe_parole_id")
    private UUID groupeParoleId;

    @Column(nullable = false)
    private String title;

    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiveSessionStatus status = LiveSessionStatus.waiting;

    @Column(name = "host_id")
    private UUID hostId;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "host_role")
    private String hostRole;

    @Column(name = "max_participants")
    private Integer maxParticipants = 20;

    @Column(name = "is_audio_only")
    private Boolean isAudioOnly = false;

    @Column(name = "agora_channel_id")
    private String agoraChannelId;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private User host;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
    private List<SessionParticipant> participants = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (startTime == null) {
            startTime = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
