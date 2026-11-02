package com.pelycan.api.service;

import com.pelycan.api.domain.LegalConsultation;
import com.pelycan.api.repository.LegalConsultationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AccompagnementJuridiqueService {

    private static final Set<String> VALID_TYPES = Set.of("consultation", "suivi", "urgence");
    private static final Set<String> VALID_MODALITES = Set.of("presentiel", "visio", "telephone");

    private final LegalConsultationRepository repository;

    public AccompagnementJuridiqueService(LegalConsultationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForUser(UUID userId, UUID consultationId) {
        return toJson(findOwned(consultationId, userId));
    }

    @Transactional
    public Map<String, Object> create(UUID userId, Map<String, Object> body) {
        String type = string(body.get("type"));
        String description = string(body.get("description"));
        String modalite = string(body.get("modalite"));
        boolean urgence = toBoolean(body.get("urgence"));

        if (type == null || !VALID_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de demande invalide");
        }
        if (description == null || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La description est obligatoire");
        }
        if (modalite == null || !VALID_MODALITES.contains(modalite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalité invalide");
        }

        LegalConsultation consultation = new LegalConsultation();
        consultation.setUserId(userId);
        consultation.setType(type);
        consultation.setDescription(description.trim());
        consultation.setModalite(modalite);
        consultation.setUrgence(urgence || "urgence".equals(type));
        consultation.setTitle(buildTitle(type, urgence));
        consultation.setStatus("en_attente");
        repository.save(consultation);
        return toJson(consultation);
    }

    @Transactional
    public void deleteForUser(UUID userId, UUID consultationId) {
        LegalConsultation consultation = findOwned(consultationId, userId);
        if (!Set.of("en_attente", "annulee").contains(consultation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les demandes en attente ou annulées peuvent être supprimées");
        }
        repository.delete(consultation);
    }

    private LegalConsultation findOwned(UUID consultationId, UUID userId) {
        return repository.findByIdAndUserId(consultationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation non trouvée"));
    }

    private String buildTitle(String type, boolean urgence) {
        if (urgence || "urgence".equals(type)) {
            return "Demande d'assistance urgente";
        }
        return switch (type) {
            case "suivi" -> "Demande de suivi juridique";
            default -> "Demande d'assistance juridique";
        };
    }

    private Map<String, Object> toJson(LegalConsultation c) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", c.getId().toString());
        json.put("userId", c.getUserId().toString());
        json.put("title", c.getTitle());
        json.put("type", c.getType());
        json.put("description", c.getDescription());
        json.put("status", c.getStatus());
        json.put("modalite", c.getModalite());
        json.put("urgence", c.isUrgence());
        json.put("date_consultation", c.getDateConsultation());
        json.put("avocat_nom", c.getAvocatNom());
        json.put("avocat_email", c.getAvocatEmail());
        json.put("avocat_telephone", c.getAvocatTelephone());
        json.put("avocat_specialite", c.getAvocatSpecialite());
        json.put("notes", c.getNotes());
        json.put("notes_internes", c.getNotesInternes());
        json.put("assignedTo", c.getAssignedTo() != null ? c.getAssignedTo().toString() : null);
        json.put("createdAt", c.getCreatedAt().toString());
        json.put("updatedAt", c.getUpdatedAt().toString());
        return json;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
