package com.pelycan.api.service;

import com.pelycan.api.domain.GroupeParoleInscription;
import com.pelycan.api.domain.GroupeParoleSession;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.GroupeParoleInscriptionRepository;
import com.pelycan.api.repository.GroupeParoleSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class GroupeParoleSessionService {

    private final GroupeParoleSessionRepository repository;
    private final GroupeParoleInscriptionRepository inscriptionRepository;

    public GroupeParoleSessionService(
            GroupeParoleSessionRepository repository,
            GroupeParoleInscriptionRepository inscriptionRepository) {
        this.repository = repository;
        this.inscriptionRepository = inscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll(User viewer) {
        Set<UUID> enrolledIds = viewer != null
                ? new HashSet<>(inscriptionRepository.findSessionIdsByUserId(viewer.getId()))
                : Set.of();

        return repository.findAllByOrderBySessionDateDescCreatedAtDesc().stream()
                .map(session -> toJson(session, enrolledIds.contains(session.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id, User viewer) {
        GroupeParoleSession session = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));
        boolean enrolled = viewer != null
                && inscriptionRepository.existsBySessionIdAndUserId(id, viewer.getId());
        return toJson(session, enrolled);
    }

    @Transactional
    public Map<String, Object> create(User creator, Map<String, Object> body) {
        requireAdminOrPro(creator);

        String title = requiredString(body, "title");
        GroupeParoleSession session = new GroupeParoleSession();
        session.setTitle(title);
        session.setDescription(stringOrEmpty(body.get("description")));
        session.setSessionType(normalizeSessionType(body.get("type")));
        session.setSessionDate(parseDate(body.get("date")));
        session.setSessionTime(normalizeTime(body.get("time")));
        session.setLocation(stringOrDefault(body.get("location"), "En ligne"));
        session.setMaxParticipants(intOrDefault(body.get("maxParticipants"), 15));
        session.setCurrentParticipants(0);
        session.setStatus("ouvert");
        session.setAnimateurName(stringOrNull(body.get("animateurName")));
        session.setPrice(parsePrice(body.get("price")));
        session.setCreatedBy(creator.getId());

        session = repository.save(session);
        return toJson(session, false);
    }

    @Transactional
    public Map<String, Object> update(UUID id, User user, Map<String, Object> body) {
        requireAdminOrPro(user);
        GroupeParoleSession session = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));

        if (body.containsKey("title")) {
            session.setTitle(requiredString(body, "title"));
        }
        if (body.containsKey("description")) {
            session.setDescription(stringOrEmpty(body.get("description")));
        }
        if (body.containsKey("type")) {
            session.setSessionType(normalizeSessionType(body.get("type")));
        }
        if (body.containsKey("date")) {
            session.setSessionDate(parseDate(body.get("date")));
        }
        if (body.containsKey("time")) {
            session.setSessionTime(normalizeTime(body.get("time")));
        }
        if (body.containsKey("location")) {
            session.setLocation(stringOrDefault(body.get("location"), "En ligne"));
        }
        if (body.containsKey("maxParticipants")) {
            session.setMaxParticipants(intOrDefault(body.get("maxParticipants"), session.getMaxParticipants()));
        }
        if (body.containsKey("status")) {
            session.setStatus(stringOrDefault(body.get("status"), session.getStatus()));
        }
        if (body.containsKey("animateurName")) {
            session.setAnimateurName(stringOrNull(body.get("animateurName")));
        }
        if (body.containsKey("price")) {
            session.setPrice(parsePrice(body.get("price")));
        }

        session = repository.save(session);
        boolean enrolled = inscriptionRepository.existsBySessionIdAndUserId(id, user.getId());
        return toJson(session, enrolled);
    }

    @Transactional
    public void enroll(UUID sessionId, User user) {
        requireAuthenticated(user);
        GroupeParoleSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));

        if (inscriptionRepository.existsBySessionIdAndUserId(sessionId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous êtes déjà inscrit");
        }

        long count = inscriptionRepository.countBySessionId(sessionId);
        if (count >= session.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session complète");
        }

        GroupeParoleInscription inscription = new GroupeParoleInscription();
        inscription.setSessionId(sessionId);
        inscription.setUserId(user.getId());
        inscriptionRepository.save(inscription);

        syncParticipantCount(session);
    }

    @Transactional
    public void unenroll(UUID sessionId, User user) {
        requireAuthenticated(user);
        GroupeParoleSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));

        GroupeParoleInscription inscription = inscriptionRepository
                .findBySessionIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vous n'êtes pas inscrit à cette session"));

        inscriptionRepository.delete(inscription);
        syncParticipantCount(session);
    }

    @Transactional
    public void delete(UUID id, User user) {
        requireAdminOrPro(user);
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée");
        }
        repository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> reschedule(UUID id, User user, Map<String, Object> body) {
        requireAdminOrPro(user);
        GroupeParoleSession session = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session non trouvée"));

        if (body.containsKey("newDate")) {
            session.setSessionDate(parseDate(body.get("newDate")));
        }
        if (body.containsKey("newTime")) {
            session.setSessionTime(normalizeTime(body.get("newTime")));
        }
        if (body.containsKey("reason")) {
            String reason = stringOrNull(body.get("reason"));
            if (reason != null) {
                String description = session.getDescription() != null ? session.getDescription() : "";
                session.setDescription(description + "\n[Report] " + reason);
            }
        }

        session = repository.save(session);
        boolean enrolled = inscriptionRepository.existsBySessionIdAndUserId(id, user.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Session reportée avec succès");
        result.put("session", toJson(session, enrolled));
        return result;
    }

    private void syncParticipantCount(GroupeParoleSession session) {
        int count = (int) inscriptionRepository.countBySessionId(session.getId());
        session.setCurrentParticipants(count);
        if (count >= session.getMaxParticipants()) {
            session.setStatus("complet");
        } else if ("complet".equals(session.getStatus())) {
            session.setStatus("ouvert");
        }
        repository.save(session);
    }

    private void requireAdminOrPro(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        if (user.getRole() != UserRole.admin && user.getRole() != UserRole.pro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
    }

    private void requireAuthenticated(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
    }

    private Map<String, Object> toJson(GroupeParoleSession s, boolean isInscrit) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", s.getId().toString());
        json.put("title", s.getTitle());
        json.put("description", s.getDescription());
        json.put("type", s.getSessionType());
        json.put("date", s.getSessionDate().toString());
        json.put("time", s.getSessionTime());
        json.put("location", s.getLocation());
        json.put("maxParticipants", s.getMaxParticipants());
        json.put("currentParticipants", s.getCurrentParticipants());
        json.put("status", s.getStatus());
        json.put("animateurName", s.getAnimateurName());
        json.put("isInscrit", isInscrit);
        if (s.getPrice() != null) {
            json.put("price", s.getPrice().doubleValue());
        }
        if (s.getCreatedBy() != null) {
            json.put("createdBy", s.getCreatedBy().toString());
        }
        if (s.getCreatedAt() != null) {
            json.put("createdAt", s.getCreatedAt().toString());
        }
        return json;
    }

    private static String requiredString(Map<String, Object> body, String key) {
        String value = stringOrNull(body.get(key));
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ requis: " + key);
        }
        return value.trim();
    }

    private static String stringOrNull(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static String stringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        String s = stringOrNull(value);
        return s == null ? defaultValue : s;
    }

    private static int intOrDefault(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static String normalizeSessionType(Object value) {
        String type = stringOrDefault(value, "visio").toLowerCase(Locale.ROOT);
        if ("presentiel".equals(type) || "présentiel".equals(type)) {
            return "presentiel";
        }
        return "visio";
    }

    private static LocalDate parseDate(Object value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date requise");
        }
        String raw = String.valueOf(value).trim();
        if (raw.contains("T")) {
            raw = raw.substring(0, raw.indexOf('T'));
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date invalide");
        }
    }

    private static String normalizeTime(Object value) {
        if (value == null) return "00:00";
        String raw = String.valueOf(value).trim();
        if (raw.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            String[] parts = raw.split(":");
            return String.format("%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return raw.length() > 16 ? raw.substring(0, 16) : raw;
    }

    private static BigDecimal parsePrice(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String raw = String.valueOf(value).replaceAll("[^\\d.,\\-]", "").replace(",", ".");
        if (raw.isBlank()) return null;
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
