package com.pelycan.api.service;

import com.pelycan.api.domain.LegalConsultation;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.LegalConsultationRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminCaseService {

    private final LegalConsultationRepository legalConsultationRepository;
    private final UserRepository userRepository;

    public AdminCaseService(
            LegalConsultationRepository legalConsultationRepository,
            UserRepository userRepository) {
        this.legalConsultationRepository = legalConsultationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUnassigned() {
        List<Map<String, Object>> cases = new ArrayList<>();
        for (LegalConsultation consultation :
                legalConsultationRepository.findByAssignedToIsNullOrderByCreatedAtDesc()) {
            if (isClosedStatus(consultation.getStatus())) {
                continue;
            }
            cases.add(toAdminCaseJson(consultation, "new"));
        }
        cases.sort(Comparator.comparing(
                (Map<String, Object> c) -> string(c.get("createdAt")),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return cases;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAssigned() {
        List<Map<String, Object>> cases = new ArrayList<>();
        for (LegalConsultation consultation :
                legalConsultationRepository.findByAssignedToIsNotNullOrderByCreatedAtDesc()) {
            if (isClosedStatus(consultation.getStatus())) {
                continue;
            }
            cases.add(toAdminCaseJson(consultation, "assigned"));
        }
        cases.sort(Comparator.comparing(
                (Map<String, Object> c) -> string(c.get("createdAt")),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return cases;
    }

    @Transactional
    public void assignCase(String caseId, UUID professionalId, String note) {
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel non trouvé"));
        if (professional.getRole() != UserRole.pro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur sélectionné n'est pas un professionnel");
        }

        UUID consultationId = parseLegalCaseId(caseId);
        LegalConsultation consultation = legalConsultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas non trouvé"));

        consultation.setAssignedTo(professionalId);
        if (consultation.getStatus() == null
                || "en_attente".equalsIgnoreCase(consultation.getStatus())
                || "new".equalsIgnoreCase(consultation.getStatus())) {
            consultation.setStatus("en_cours");
        }
        if (note != null && !note.isBlank()) {
            consultation.setNotesInternes(note.trim());
        }
        legalConsultationRepository.save(consultation);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> caseStatistics() {
        long total = legalConsultationRepository.count();
        long unassigned = legalConsultationRepository.countByAssignedToIsNull();
        long assigned = legalConsultationRepository.countByAssignedToIsNotNull();
        long urgent = legalConsultationRepository.countByUrgenceTrue();
        long inProgress = legalConsultationRepository.countByStatusIgnoreCase("en_cours");
        long completed = legalConsultationRepository.countByStatusIgnoreCase("termine")
                + legalConsultationRepository.countByStatusIgnoreCase("completed");

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("unassigned", unassigned);
        stats.put("assigned", assigned);
        stats.put("inProgress", inProgress);
        stats.put("completed", completed);
        stats.put("urgent", urgent);
        return stats;
    }

    @Transactional(readOnly = true)
    public long countActiveCasesForProfessional(UUID professionalId) {
        return legalConsultationRepository
                .findByAssignedToOrderByCreatedAtDesc(professionalId)
                .stream()
                .filter(c -> !isClosedStatus(c.getStatus()))
                .count();
    }

    private Map<String, Object> toAdminCaseJson(LegalConsultation consultation, String adminStatus) {
        User client = userRepository.findById(consultation.getUserId()).orElse(null);
        User professional = consultation.getAssignedTo() != null
                ? userRepository.findById(consultation.getAssignedTo()).orElse(null)
                : null;
        String caseId = "legal-" + consultation.getId();

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", caseId);
        json.put("caseId", caseId);
        json.put("title", consultation.getTitle());
        json.put("description", consultation.getDescription());
        json.put("type", consultation.getType() != null ? consultation.getType() : "legal");
        json.put("priority", consultation.isUrgence() ? "high" : "medium");
        json.put("is_emergency", consultation.isUrgence());
        json.put("isEmergency", consultation.isUrgence());
        json.put("status", adminStatus);
        json.put("userId", consultation.getUserId() != null ? consultation.getUserId().toString() : null);
        json.put("professionalId",
                consultation.getAssignedTo() != null ? consultation.getAssignedTo().toString() : null);
        json.put("createdAt", consultation.getCreatedAt());
        json.put("updatedAt", consultation.getUpdatedAt());
        if (client != null) {
            json.put("user", toUserJson(client));
        }
        if (professional != null) {
            json.put("professional", toProfessionalJson(professional));
            json.put("professionalInfo", toProfessionalJson(professional));
        }
        return json;
    }

    private Map<String, Object> toUserJson(User user) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", user.getId().toString());
        json.put("firstName", user.getFirstName());
        json.put("lastName", user.getLastName());
        json.put("name", clientName(user));
        json.put("email", user.getEmail());
        return json;
    }

    private Map<String, Object> toProfessionalJson(User user) {
        Map<String, Object> json = toUserJson(user);
        json.put("firstName", user.getFirstName());
        json.put("lastName", user.getLastName());
        return json;
    }

    private static UUID parseLegalCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de cas invalide");
        }
        String raw = caseId.startsWith("legal-") ? caseId.substring(6) : caseId;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de cas invalide");
        }
    }

    private static boolean isClosedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toLowerCase();
        return normalized.equals("completed")
                || normalized.equals("termine")
                || normalized.equals("cancelled")
                || normalized.equals("annule")
                || normalized.equals("rejected");
    }

    private static String clientName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }
}
