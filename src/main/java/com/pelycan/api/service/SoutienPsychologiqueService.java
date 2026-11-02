package com.pelycan.api.service;

import com.pelycan.api.domain.ConsultationRequest;
import com.pelycan.api.domain.MoodEntry;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.ConsultationRequestRepository;
import com.pelycan.api.repository.MoodEntryRepository;
import com.pelycan.api.repository.UserRepository;
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
public class SoutienPsychologiqueService {

    private static final Set<String> VALID_MOODS = Set.of(
            "Bien", "Neutre", "Triste", "Anxieux", "Stressé");

    private final UserRepository userRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final ConsultationRequestRepository consultationRequestRepository;

    public SoutienPsychologiqueService(
            UserRepository userRepository,
            MoodEntryRepository moodEntryRepository,
            ConsultationRequestRepository consultationRequestRepository) {
        this.userRepository = userRepository;
        this.moodEntryRepository = moodEntryRepository;
        this.consultationRequestRepository = consultationRequestRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProfessionals(String type) {
        List<Map<String, Object>> professionals = userRepository
                .findByRoleAndStatusOrderByCreatedAtDesc(UserRole.pro, "active")
                .stream()
                .filter(user -> matchesType(user, type))
                .limit(50)
                .map(this::toProfessionalJson)
                .toList();

        return Map.of(
                "success", true,
                "professionals", professionals);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfessional(String id) {
        UUID professionalId;
        try {
            professionalId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant professionnel invalide");
        }

        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel non trouvé"));

        if (professional.getRole() != UserRole.pro) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel non trouvé");
        }

        return Map.of(
                "success", true,
                "professional", toProfessionalJson(professional));
    }

    @Transactional
    public Map<String, Object> saveMood(UUID userId, Map<String, Object> body) {
        String mood = string(body.get("mood"));
        if (mood == null || mood.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'humeur est obligatoire");
        }
        if (!VALID_MOODS.contains(mood)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Humeur invalide");
        }

        MoodEntry entry = new MoodEntry();
        entry.setUserId(userId);
        entry.setMood(mood);
        entry.setNote(string(body.get("note")));
        moodEntryRepository.save(entry);

        return Map.of(
                "success", true,
                "message", "Humeur enregistrée avec succès",
                "data", toMoodJson(entry));
    }

    @Transactional
    public Map<String, Object> requestConsultation(UUID userId, Map<String, Object> body) {
        String professionalIdRaw = string(first(body, "professional_id", "professionalId"));
        String message = string(body.get("message"));

        if (professionalIdRaw == null || message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le professionnel et le message sont obligatoires");
        }

        UUID professionalId;
        try {
            professionalId = UUID.fromString(professionalIdRaw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant professionnel invalide");
        }

        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel non trouvé"));

        if (professional.getRole() != UserRole.pro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur non professionnel");
        }

        ConsultationRequest request = new ConsultationRequest();
        request.setUserId(userId);
        request.setProfessionalId(professionalId);
        request.setMessage(message.trim());
        request.setPreferredDate(string(first(body, "preferred_date", "preferredDate")));
        consultationRequestRepository.save(request);

        return Map.of(
                "success", true,
                "message", "Demande envoyée avec succès");
    }

    private boolean matchesType(User user, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        return true;
    }

    private Map<String, Object> toProfessionalJson(User user) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", user.getId().toString());
        json.put("first_name", user.getFirstName());
        json.put("last_name", user.getLastName());
        json.put("email", user.getEmail());
        json.put("phone_number", user.getPhoneNumber());
        json.put("speciality", null);
        json.put("status", user.getStatus());
        json.put("profile_picture", null);
        json.put("availability", null);
        json.put("rating", 5.0);
        json.put("total_cases", 0);
        json.put("active_cases", 0);
        return json;
    }

    private Map<String, Object> toMoodJson(MoodEntry entry) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", entry.getId().toString());
        json.put("mood", entry.getMood());
        json.put("note", entry.getNote());
        json.put("created_at", entry.getCreatedAt().toString());
        return json;
    }

    private static Object first(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return body.get(key);
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }
}
