package com.pelycan.api.service;

import com.pelycan.api.domain.UserEvidence;
import com.pelycan.api.repository.UserEvidenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final Set<String> VALID_TYPES =
            Set.of("photo", "audio", "video", "document", "other");

    private final UserEvidenceRepository repository;

    public EvidenceService(UserEvidenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(UUID userId, String type) {
        List<UserEvidence> items = type == null || type.isBlank()
                ? repository.findByUserIdOrderByCreatedAtDesc(userId)
                : repository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type.trim());
        return items.stream().map(this::toJson).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForUser(UUID userId, UUID evidenceId) {
        return toJson(findOwned(evidenceId, userId));
    }

    @Transactional
    public Map<String, Object> create(UUID userId, Map<String, Object> body) {
        UUID requestedUserId = parseUserId(body.get("userId"));
        if (requestedUserId != null && !requestedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        String type = requiredString(body, "type");
        if (!VALID_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de preuve invalide");
        }

        String fileName = requiredString(body, "fileName");
        String fileUrl = requiredString(body, "fileUrl");

        UserEvidence evidence = new UserEvidence();
        evidence.setUserId(userId);
        evidence.setType(type);
        evidence.setFileName(fileName);
        evidence.setFileUrl(fileUrl);
        evidence.setFileSize(toInteger(body.get("fileSize")));
        evidence.setMimeType(string(body.get("mimeType")));
        evidence.setDescription(string(body.get("description")));
        evidence.setIncidentDate(parseInstant(body.get("incidentDate")));
        evidence.setLocation(string(body.get("location")));
        evidence.setEncrypted(toBoolean(body.get("encrypted"), true));
        evidence.setSharedWith(parseSharedWith(body.get("sharedWith")));
        repository.save(evidence);
        return toJson(evidence);
    }

    @Transactional
    public void deleteForUser(UUID userId, UUID evidenceId) {
        UserEvidence evidence = findOwned(evidenceId, userId);
        repository.delete(evidence);
    }

    private UserEvidence findOwned(UUID evidenceId, UUID userId) {
        return repository.findByIdAndUserId(evidenceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preuve non trouvée"));
    }

    private Map<String, Object> toJson(UserEvidence evidence) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", evidence.getId().toString());
        json.put("userId", evidence.getUserId().toString());
        json.put("type", evidence.getType());
        json.put("fileName", evidence.getFileName());
        json.put("fileUrl", evidence.getFileUrl());
        json.put("fileSize", evidence.getFileSize());
        json.put("mimeType", evidence.getMimeType());
        json.put("description", evidence.getDescription());
        json.put("incidentDate", evidence.getIncidentDate() != null
                ? evidence.getIncidentDate().toString()
                : null);
        json.put("location", evidence.getLocation());
        json.put("encrypted", evidence.isEncrypted());
        json.put("sharedWith", evidence.getSharedWith() != null
                ? evidence.getSharedWith()
                : List.of());
        json.put("createdAt", evidence.getCreatedAt().toString());
        json.put("updatedAt", evidence.getUpdatedAt().toString());
        return json;
    }

    private static UUID parseUserId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId invalide");
        }
    }

    private static String requiredString(Map<String, Object> body, String key) {
        String value = string(body.get(key));
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ requis: " + key);
        }
        return value;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseSharedWith(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
