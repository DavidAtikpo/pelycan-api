package com.pelycan.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_participants")
@Getter
@Setter
public class SessionParticipant {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "agora_uid")
    private Long agoraUid;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_role")
    private String userRole = "user";

    @Column(name = "is_audio_muted")
    private Boolean isAudioMuted = false;

    @Column(name = "is_video_enabled")
    private Boolean isVideoEnabled = true;

    @Column(name = "is_hand_raised")
    private Boolean isHandRaised = false;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private LiveSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
