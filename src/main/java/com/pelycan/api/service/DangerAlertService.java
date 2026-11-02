package com.pelycan.api.service;

import com.pelycan.api.domain.DangerAlert;
import com.pelycan.api.domain.User;
import com.pelycan.api.repository.DangerAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class DangerAlertService {

    private final DangerAlertRepository repository;

    public DangerAlertService(DangerAlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Map<String, Object> create(User user, Map<String, Object> body, boolean voiceDetection) {
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) body.get("location");
        if (location == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les coordonnées de localisation sont requises");
        }
        Double lat = dbl(location.get("latitude"));
        Double lng = dbl(location.get("longitude"));
        if (lat == null || lng == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les coordonnées de localisation sont requises");
        }
        DangerAlert alert = new DangerAlert();
        alert.setUserId(user.getId());
        alert.setUserName(userDisplayName(user));
        alert.setLatitude(lat);
        alert.setLongitude(lng);
        alert.setAccuracy(dbl(location.get("accuracy")));
        alert.setStatus("active");
        if (voiceDetection) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
            alert.setAlertType("voice_detection");
            alert.setMetadata(buildVoiceMetadata(metadata));
            alert.setPriority(calculatePriority(metadata));
        }
        repository.save(alert);
        Map<String, Object> formatted = toClientJson(alert, voiceDetection);
        String msg = voiceDetection
                ? "Alerte de détection vocale créée avec succès"
                : "Alerte créée avec succès";
        return Map.of("message", msg, "data", formatted);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAll() {
        List<Map<String, Object>> alerts = repository.findAllByOrderByAlertTimestampDesc().stream()
                .map(a -> toClientJson(a, "voice_detection".equals(a.getAlertType())))
                .toList();
        return Map.of("alerts", alerts);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> activeSummary() {
        Optional<DangerAlert> latest = repository.findFirstByStatusOrderByAlertTimestampDesc("active");
        return Map.of(
                "hasActiveAlerts", latest.isPresent(),
                "alertId", latest.map(DangerAlert::getId).orElse(null),
                "viewed", latest.map(DangerAlert::getViewedByAdmin).orElse(false));
    }

    @Transactional
    public void addMessage(UUID alertId, UUID adminId, String message) {
        DangerAlert alert = repository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        List<Map<String, Object>> messages = alert.getMessages() != null
                ? new ArrayList<>(alert.getMessages())
                : new ArrayList<>();
        messages.add(Map.of(
                "text", message,
                "senderId", adminId.toString(),
                "timestamp", Instant.now().toString()));
        alert.setMessages(messages);
        repository.save(alert);
    }

    @Transactional
    public void process(UUID alertId) {
        DangerAlert alert = repository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        alert.setStatus("processed");
        repository.save(alert);
    }

    @Transactional
    public DangerAlert markViewed(UUID id) {
        DangerAlert alert = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        alert.setViewedByAdmin(true);
        return repository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForAdmin() {
        return repository.findAllByOrderByAlertTimestampDesc().stream()
                .map(this::toEmergencyJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPendingForAdmin() {
        return repository.findAllByOrderByAlertTimestampDesc().stream()
                .filter(a -> "active".equalsIgnoreCase(a.getStatus()))
                .map(this::toEmergencyJson)
                .toList();
    }

    @Transactional
    public void updateStatus(UUID alertId, String status) {
        DangerAlert alert = repository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        alert.setStatus(mapEmergencyStatus(status));
        repository.save(alert);
    }

    @Transactional
    public void assignProfessional(UUID alertId, UUID professionalId) {
        DangerAlert alert = repository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        Map<String, Object> metadata = alert.getMetadata() != null
                ? new LinkedHashMap<>(alert.getMetadata())
                : new LinkedHashMap<>();
        metadata.put("assignedProfessionalId", professionalId.toString());
        alert.setMetadata(metadata);
        repository.save(alert);
    }

    private Map<String, Object> toEmergencyJson(DangerAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("userId", alert.getUserId());
        m.put("title", buildEmergencyTitle(alert));
        m.put("description", buildEmergencyDescription(alert));
        m.put("user", Map.of("name", alert.getUserName() != null ? alert.getUserName() : "Utilisateur"));
        m.put("userName", alert.getUserName());
        m.put("type", alert.getAlertType());
        m.put("metadata", alert.getMetadata());
        m.put("priority", alert.getPriority() != null ? alert.getPriority() : "medium");
        m.put("location", Map.of(
                "latitude", alert.getLatitude(),
                "longitude", alert.getLongitude(),
                "accuracy", alert.getAccuracy()));
        m.put("timestamp", alert.getAlertTimestamp());
        m.put("status", mapStatusForClient(alert.getStatus()));
        return m;
    }

    private static String buildEmergencyTitle(DangerAlert alert) {
        if ("voice_detection".equals(alert.getAlertType())) {
            return "Détection vocale";
        }
        return "Alerte danger";
    }

    private static String buildEmergencyDescription(DangerAlert alert) {
        if ("voice_detection".equals(alert.getAlertType()) && alert.getMetadata() != null) {
            Object level = alert.getMetadata().get("soundLevel");
            return level != null ? "Niveau sonore détecté: " + level : "Alerte de détection vocale";
        }
        return "Alerte de danger signalée par l'utilisateur";
    }

    private static String mapStatusForClient(String status) {
        if (status == null) {
            return "pending";
        }
        return switch (status.toLowerCase()) {
            case "active" -> "pending";
            case "processed" -> "resolved";
            default -> status;
        };
    }

    private static String mapEmergencyStatus(String status) {
        if (status == null) {
            return "active";
        }
        return switch (status.toLowerCase()) {
            case "resolved", "closed", "processed" -> "processed";
            case "pending", "new", "active" -> "active";
            default -> status.toLowerCase();
        };
    }

    private Map<String, Object> toClientJson(DangerAlert alert, boolean voice) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("userId", alert.getUserId());
        m.put("userName", alert.getUserName());
        if (voice) {
            m.put("type", "voice_detection");
            m.put("metadata", alert.getMetadata());
            m.put("priority", alert.getPriority());
        }
        m.put("location", Map.of(
                "latitude", alert.getLatitude(),
                "longitude", alert.getLongitude(),
                "accuracy", alert.getAccuracy()));
        m.put("timestamp", alert.getAlertTimestamp());
        m.put("status", alert.getStatus());
        return m;
    }

    private static String userDisplayName(User user) {
        String name = (user.getFirstName() + " " + user.getLastName()).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private static Map<String, Object> buildVoiceMetadata(Map<String, Object> metadata) {
        Map<String, Object> voice = new LinkedHashMap<>();
        voice.put("type", "voice_detection");
        if (metadata == null) {
            return voice;
        }
        voice.put("sensitivity", metadata.getOrDefault("sensitivity", "medium"));
        voice.put("soundLevel", metadata.getOrDefault("soundLevel", 0));
        voice.put("threshold", metadata.getOrDefault("threshold", 65));
        voice.put("detectionTime", metadata.getOrDefault("detectionTime", Instant.now().toString()));
        return voice;
    }

    private static String calculatePriority(Map<String, Object> metadata) {
        if (metadata == null || metadata.get("soundLevel") == null) {
            return "medium";
        }
        double level = dbl(metadata.get("soundLevel"));
        if (level > 80) {
            return "critical";
        }
        if (level > 70) {
            return "high";
        }
        return "medium";
    }

    private static Double dbl(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(o));
    }
}
