package com.pelycan.api.service;

import com.pelycan.api.domain.*;
import com.pelycan.api.repository.LiveSessionRepository;
import com.pelycan.api.repository.SessionParticipantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class LiveSessionService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    private final LiveSessionRepository liveSessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final AgoraTokenService agoraTokenService;

    public LiveSessionService(
            LiveSessionRepository liveSessionRepository,
            SessionParticipantRepository participantRepository,
            AgoraTokenService agoraTokenService) {
        this.liveSessionRepository = liveSessionRepository;
        this.participantRepository = participantRepository;
        this.agoraTokenService = agoraTokenService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return liveSessionRepository.findAllWithDetails().stream().map(this::toSessionJson).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActive() {
        return liveSessionRepository.findByStatusesWithDetails(
                        List.of(LiveSessionStatus.waiting, LiveSessionStatus.live))
                .stream()
                .map(this::toSessionJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id) {
        LiveSession s = liveSessionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));
        return toSessionJson(s);
    }

    @Transactional
    public Map<String, Object> create(User host, Map<String, Object> body) {
        String title = stringVal(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le titre est requis");
        }
        UUID groupeId = parseUuid(body.get("groupe_parole_id"));
        LiveSession session = new LiveSession();
        session.setGroupeParoleId(groupeId);
        session.setTitle(title.trim());
        session.setType(stringVal(body.get("type")) != null ? stringVal(body.get("type")) : "Général");
        session.setStatus(LiveSessionStatus.waiting);
        session.setHostId(host.getId());
        session.setHostName(displayName(host));
        session.setHostRole(host.getRole() == UserRole.admin ? "admin" : "professionnel");
        session.setMaxParticipants(intOrDefault(body.get("max_participants"), 20));
        session.setIsAudioOnly(bool(body.get("is_audio_only")));
        session.setAgoraChannelId("session-" + System.currentTimeMillis());
        session.setStartTime(Instant.now());
        liveSessionRepository.save(session);

        SessionParticipant hostParticipant = new SessionParticipant();
        hostParticipant.setSessionId(session.getId());
        hostParticipant.setUserId(host.getId());
        hostParticipant.setUserName(session.getHostName());
        hostParticipant.setUserRole(session.getHostRole());
        hostParticipant.setIsVideoEnabled(!session.getIsAudioOnly());
        participantRepository.save(hostParticipant);

        session.setStatus(LiveSessionStatus.live);
        liveSessionRepository.save(session);

        int uid = AgoraTokenService.agoraUidFromUser(host.getId());
        String token = agoraTokenService.buildPublisherToken(session.getAgoraChannelId(), uid);
        Map<String, Object> out = toSessionJson(session);
        out.put("agora_token", token);
        out.put("agora_channel_id", session.getAgoraChannelId());
        out.put("agora_uid", uid);
        out.put("meeting_link", null);
        out.put("participants", List.of());
        return out;
    }

    @Transactional
    public Map<String, Object> join(UUID sessionId, User user) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));
        if (session.getStatus() == LiveSessionStatus.ended) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette session est terminée");
        }
        long count = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        if (count >= session.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La session est complète");
        }
        int agoraUid = AgoraTokenService.agoraUidFromUser(user.getId());
        SessionParticipant participant = participantRepository
                .findBySessionIdAndUserIdAndLeftAtIsNull(sessionId, user.getId())
                .orElseGet(() -> {
                    SessionParticipant p = new SessionParticipant();
                    p.setSessionId(sessionId);
                    p.setUserId(user.getId());
                    p.setUserName(displayName(user));
                    p.setUserRole(user.getRole().name());
                    p.setIsVideoEnabled(!session.getIsAudioOnly());
                    return p;
                });
        participant.setAgoraUid((long) agoraUid);
        participantRepository.save(participant);

        if (session.getAgoraChannelId() == null || session.getAgoraChannelId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La session n'a pas de canal Agora configuré");
        }
        String token = agoraTokenService.buildPublisherToken(session.getAgoraChannelId(), agoraUid);
        return Map.of("token", token, "uid", agoraUid, "channel_id", session.getAgoraChannelId());
    }

    @Transactional
    public void leave(UUID sessionId, UUID userId) {
        participantRepository.findBySessionIdAndUserIdAndLeftAtIsNull(sessionId, userId)
                .ifPresent(p -> {
                    p.setLeftAt(Instant.now());
                    participantRepository.save(p);
                });
    }

    @Transactional
    public void end(UUID sessionId, User user) {
        LiveSession session = liveSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));
        if (!session.getHostId().equals(user.getId()) && user.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à terminer cette session");
        }
        session.setStatus(LiveSessionStatus.ended);
        session.setEndTime(Instant.now());
        liveSessionRepository.save(session);
        participantRepository.markAllLeft(sessionId, Instant.now());
    }

    @Transactional
    public void updateAudio(UUID sessionId, UUID userId, boolean muted) {
        SessionParticipant p = activeParticipant(sessionId, userId);
        p.setIsAudioMuted(muted);
        participantRepository.save(p);
    }

    @Transactional
    public void updateVideo(UUID sessionId, UUID userId, boolean enabled) {
        SessionParticipant p = activeParticipant(sessionId, userId);
        p.setIsVideoEnabled(enabled);
        participantRepository.save(p);
    }

    @Transactional
    public void updateHand(UUID sessionId, UUID userId, boolean raised) {
        SessionParticipant p = activeParticipant(sessionId, userId);
        p.setIsHandRaised(raised);
        participantRepository.save(p);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getParticipantByAgoraUid(UUID sessionId, long agoraUid) {
        SessionParticipant p = participantRepository
                .findBySessionIdAndAgoraUidAndLeftAtIsNull(sessionId, agoraUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant non trouvé"));
        return Map.of(
                "success", true,
                "participant", Map.of(
                        "id", p.getId(),
                        "agoraUid", p.getAgoraUid(),
                        "userId", p.getUserId(),
                        "userName", p.getUserName(),
                        "userRole", p.getUserRole(),
                        "joinedAt", p.getJoinedAt()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listParticipants(UUID sessionId) {
        List<Map<String, Object>> list = participantRepository.findBySessionIdAndLeftAtIsNull(sessionId).stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "agoraUid", p.getAgoraUid(),
                        "userId", p.getUserId(),
                        "userName", p.getUserName(),
                        "userRole", p.getUserRole(),
                        "joinedAt", p.getJoinedAt()))
                .toList();
        return Map.of("success", true, "participants", list);
    }

    private SessionParticipant activeParticipant(UUID sessionId, UUID userId) {
        return participantRepository.findBySessionIdAndUserIdAndLeftAtIsNull(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant non trouvé"));
    }

    private Map<String, Object> toSessionJson(LiveSession s) {
        List<SessionParticipant> active = s.getParticipants() != null
                ? s.getParticipants().stream().filter(p -> p.getLeftAt() == null).toList()
                : List.of();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("groupe_parole_id", s.getGroupeParoleId());
        m.put("title", s.getTitle());
        m.put("type", s.getType());
        m.put("status", s.getStatus().name());
        m.put("host_id", s.getHostId());
        m.put("host_name", s.getHostName());
        m.put("host_role", s.getHostRole());
        m.put("max_participants", s.getMaxParticipants());
        m.put("is_audio_only", s.getIsAudioOnly());
        m.put("start_time", s.getStartTime());
        m.put("started_at", s.getStartTime());
        m.put("end_time", s.getEndTime());
        m.put("ended_at", s.getEndTime());
        m.put("participants", active);
        m.put("current_participants", active.size());
        m.put("participant_count", active.size());
        m.put("agora_channel_id", s.getAgoraChannelId());
        return m;
    }

    private static String displayName(User u) {
        if (u.getFirstName() != null || u.getLastName() != null) {
            return ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                    + (u.getLastName() != null ? u.getLastName() : "")).trim();
        }
        return u.getEmail();
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw);
        return UUID_PATTERN.matcher(s).matches() ? UUID.fromString(s) : null;
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int intOrDefault(Object o, int def) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o == null) {
            return def;
        }
        return Integer.parseInt(String.valueOf(o));
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return o != null && Boolean.parseBoolean(String.valueOf(o));
    }
}
