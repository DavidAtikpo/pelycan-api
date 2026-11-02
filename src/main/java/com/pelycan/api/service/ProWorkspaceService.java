package com.pelycan.api.service;

import com.pelycan.api.domain.ConsultationRequest;
import com.pelycan.api.domain.LegalConsultation;
import com.pelycan.api.domain.ProCaseNote;
import com.pelycan.api.domain.ProfessionalAppointment;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.ConsultationRequestRepository;
import com.pelycan.api.repository.LegalConsultationRepository;
import com.pelycan.api.repository.ProCaseNoteRepository;
import com.pelycan.api.repository.ProfessionalAppointmentRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProWorkspaceService {

    private final ConsultationRequestRepository consultationRequestRepository;
    private final LegalConsultationRepository legalConsultationRepository;
    private final ProfessionalAppointmentRepository appointmentRepository;
    private final ProCaseNoteRepository caseNoteRepository;
    private final UserRepository userRepository;

    public ProWorkspaceService(
            ConsultationRequestRepository consultationRequestRepository,
            LegalConsultationRepository legalConsultationRepository,
            ProfessionalAppointmentRepository appointmentRepository,
            ProCaseNoteRepository caseNoteRepository,
            UserRepository userRepository) {
        this.consultationRequestRepository = consultationRequestRepository;
        this.legalConsultationRepository = legalConsultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.caseNoteRepository = caseNoteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActiveCases(UUID professionalId) {
        requireProfessional(professionalId);
        List<Map<String, Object>> cases = new ArrayList<>();

        for (ConsultationRequest request :
                consultationRequestRepository.findByProfessionalIdOrderByCreatedAtDesc(professionalId)) {
            if (isClosedStatus(request.getStatus())) {
                continue;
            }
            cases.add(toPsychCaseJson(request));
        }

        for (LegalConsultation consultation :
                legalConsultationRepository.findByAssignedToOrderByCreatedAtDesc(professionalId)) {
            if (isClosedStatus(consultation.getStatus())) {
                continue;
            }
            cases.add(toLegalCaseJson(consultation));
        }

        cases.sort(Comparator.comparing(
                (Map<String, Object> c) -> string(c.get("updatedAt")),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return cases;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCaseDetails(UUID professionalId, String caseId) {
        requireProfessional(professionalId);
        CaseRef ref = parseCaseId(caseId);

        if ("psych".equals(ref.type())) {
            ConsultationRequest request = consultationRequestRepository
                    .findByIdAndProfessionalId(ref.id(), professionalId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas non trouvé"));
            Map<String, Object> json = toPsychCaseJson(request);
            json.put("notes", loadNotes(caseId));
            return json;
        }

        LegalConsultation consultation = legalConsultationRepository.findById(ref.id())
                .filter(c -> professionalId.equals(c.getAssignedTo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas non trouvé"));
        Map<String, Object> json = toLegalCaseJson(consultation);
        json.put("notes", loadNotes(caseId));
        return json;
    }

    @Transactional
    public void updateCaseStatus(UUID professionalId, String caseId, String status) {
        requireProfessional(professionalId);
        CaseRef ref = parseCaseId(caseId);

        if ("psych".equals(ref.type())) {
            ConsultationRequest request = consultationRequestRepository
                    .findByIdAndProfessionalId(ref.id(), professionalId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas non trouvé"));
            request.setStatus(normalizeStatus(status));
            consultationRequestRepository.save(request);
            return;
        }

        LegalConsultation consultation = legalConsultationRepository.findById(ref.id())
                .filter(c -> professionalId.equals(c.getAssignedTo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cas non trouvé"));
        consultation.setStatus(normalizeStatus(status));
        legalConsultationRepository.save(consultation);
    }

    @Transactional
    public Map<String, Object> addCaseNote(UUID professionalId, String caseId, String note) {
        requireProfessional(professionalId);
        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note vide");
        }
        parseCaseId(caseId);

        ProCaseNote caseNote = new ProCaseNote();
        caseNote.setCaseId(caseId);
        caseNote.setCaseType(caseId.startsWith("legal-") ? "legal" : "psych");
        caseNote.setProfessionalId(professionalId);
        caseNote.setNote(note.trim());
        caseNoteRepository.save(caseNote);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", caseNote.getId().toString());
        json.put("note", caseNote.getNote());
        json.put("createdAt", caseNote.getCreatedAt().toString());
        return json;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAppointments(UUID professionalId) {
        requireProfessional(professionalId);
        return appointmentRepository.findByProfessionalIdOrderByAppointmentDateDesc(professionalId).stream()
                .map(this::toAppointmentJson)
                .toList();
    }

    @Transactional
    public Map<String, Object> createAppointment(UUID professionalId, Map<String, Object> body) {
        requireProfessional(professionalId);

        UUID userId = parseUuid(first(body, "userId", "user_id", "clientId"), "utilisateur");
        Instant appointmentDate = parseInstant(first(body, "appointment_date", "date", "scheduledAt"));
        if (appointmentDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date de rendez-vous requise");
        }

        ProfessionalAppointment appointment = new ProfessionalAppointment();
        appointment.setProfessionalId(professionalId);
        appointment.setUserId(userId);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setDurationMinutes(toInt(body.get("duration_minutes"), 60));
        appointment.setStatus(string(body.get("status")) != null ? string(body.get("status")) : "confirmed");
        appointment.setTitle(string(body.get("title")));
        appointment.setNotes(string(body.get("notes")));
        appointment.setLocation(string(body.get("location")));
        appointmentRepository.save(appointment);
        return toAppointmentJson(appointment);
    }

    @Transactional
    public Map<String, Object> updateAppointmentStatus(
            UUID professionalId, UUID appointmentId, String status) {
        requireProfessional(professionalId);
        ProfessionalAppointment appointment = appointmentRepository
                .findByIdAndProfessionalId(appointmentId, professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
        appointment.setStatus(normalizeStatus(status));
        appointmentRepository.save(appointment);
        return toAppointmentJson(appointment);
    }

    private List<Map<String, Object>> loadNotes(String caseId) {
        return caseNoteRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(note -> {
                    Map<String, Object> json = new LinkedHashMap<>();
                    json.put("id", note.getId().toString());
                    json.put("note", note.getNote());
                    json.put("createdAt", note.getCreatedAt().toString());
                    return json;
                })
                .toList();
    }

    private Map<String, Object> toPsychCaseJson(ConsultationRequest request) {
        User client = userRepository.findById(request.getUserId()).orElse(null);
        String caseId = "psych-" + request.getId();

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", caseId);
        json.put("caseId", caseId);
        json.put("type", "psych");
        json.put("caseType", "Psychologique");
        json.put("title", "Demande de consultation");
        json.put("description", request.getMessage());
        json.put("status", request.getStatus());
        json.put("priority", request.getPriority());
        json.put("preferredDate", request.getPreferredDate());
        json.put("createdAt", request.getCreatedAt().toString());
        json.put("updatedAt", request.getUpdatedAt().toString());
        json.put("clientName", clientName(client));
        if (client != null) {
            json.put("user", toUserJson(client));
            json.put("client", toUserJson(client));
        }
        return json;
    }

    private Map<String, Object> toLegalCaseJson(LegalConsultation consultation) {
        User client = userRepository.findById(consultation.getUserId()).orElse(null);
        String caseId = "legal-" + consultation.getId();

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", caseId);
        json.put("caseId", caseId);
        json.put("type", "legal");
        json.put("caseType", "Juridique");
        json.put("title", consultation.getTitle());
        json.put("description", consultation.getDescription());
        json.put("status", consultation.getStatus());
        json.put("priority", consultation.isUrgence() ? "high" : "medium");
        json.put("modalite", consultation.getModalite());
        json.put("urgence", consultation.isUrgence());
        json.put("createdAt", consultation.getCreatedAt().toString());
        json.put("updatedAt", consultation.getUpdatedAt().toString());
        json.put("clientName", clientName(client));
        if (client != null) {
            json.put("user", toUserJson(client));
            json.put("client", toUserJson(client));
        }
        return json;
    }

    private Map<String, Object> toAppointmentJson(ProfessionalAppointment appointment) {
        User client = userRepository.findById(appointment.getUserId()).orElse(null);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", appointment.getId().toString());
        json.put("professionalId", appointment.getProfessionalId().toString());
        json.put("userId", appointment.getUserId().toString());
        json.put("appointment_date", appointment.getAppointmentDate().toString());
        json.put("date", appointment.getAppointmentDate().toString());
        json.put("scheduledAt", appointment.getAppointmentDate().toString());
        json.put("startTime", appointment.getAppointmentDate().toString());
        json.put("duration_minutes", appointment.getDurationMinutes());
        json.put("status", appointment.getStatus());
        json.put("title", appointment.getTitle());
        json.put("notes", appointment.getNotes());
        json.put("location", appointment.getLocation());
        json.put("rejection_reason", appointment.getRejectionReason());
        json.put("createdAt", appointment.getCreatedAt().toString());
        json.put("updatedAt", appointment.getUpdatedAt().toString());
        if (client != null) {
            json.put("clientName", clientName(client));
            json.put("user", toUserJson(client));
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
        json.put("phone", user.getPhoneNumber());
        json.put("telephone", user.getPhoneNumber());
        return json;
    }

    private void requireProfessional(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        if (user.getRole() != UserRole.pro && user.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux professionnels");
        }
    }

    private record CaseRef(String type, UUID id) {}

    private static CaseRef parseCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de cas invalide");
        }
        if (caseId.startsWith("psych-")) {
            return new CaseRef("psych", UUID.fromString(caseId.substring(6)));
        }
        if (caseId.startsWith("legal-")) {
            return new CaseRef("legal", UUID.fromString(caseId.substring(6)));
        }
        try {
            return new CaseRef("psych", UUID.fromString(caseId));
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

    private static String normalizeStatus(String status) {
        return status == null ? "pending" : status.trim().toLowerCase();
    }

    private static String clientName(User user) {
        if (user == null) {
            return "Client";
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
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

    private static UUID parseUuid(Object raw, String label) {
        String value = string(raw);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " manquant");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " invalide");
        }
    }

    private static Object first(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return body.get(key);
            }
        }
        return null;
    }
}
