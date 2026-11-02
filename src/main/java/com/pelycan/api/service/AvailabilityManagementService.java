package com.pelycan.api.service;

import com.pelycan.api.domain.ProfessionalAvailability;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.ProfessionalAvailabilityRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AvailabilityManagementService {

    private final ProfessionalAvailabilityRepository repository;
    private final UserRepository userRepository;

    public AvailabilityManagementService(
            ProfessionalAvailabilityRepository repository,
            UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(UUID professionalId, String availabilityType) {
        requireProfessional(professionalId);
        List<Map<String, Object>> availabilities =
                repository.findByProfessionalIdAndAvailabilityTypeOrderByCreatedAtDesc(
                                professionalId, availabilityType)
                        .stream()
                        .map(this::toJson)
                        .toList();
        return Map.of("availabilities", availabilities);
    }

    @Transactional
    public Map<String, Object> create(UUID professionalId, Map<String, Object> body) {
        requireProfessional(professionalId);
        String type = string(first(body, "availability_type", "availabilityType"));
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de disponibilité requis");
        }

        ProfessionalAvailability availability = new ProfessionalAvailability();
        availability.setProfessionalId(professionalId);
        availability.setAvailabilityType(type);
        availability.setDayOfWeek(toInteger(body.get("day_of_week")));
        availability.setStartTime(string(body.get("start_time")));
        availability.setEndTime(string(body.get("end_time")));
        availability.setStartDate(parseDate(body.get("start_date")));
        availability.setEndDate(parseDate(body.get("end_date")));
        availability.setRecurring(toBoolean(body.get("is_recurring"), false));
        availability.setRecurrencePattern(string(body.get("recurrence_pattern")));
        availability.setNotes(string(body.get("notes")));
        repository.save(availability);
        return toJson(availability);
    }

    @Transactional
    public void delete(UUID professionalId, UUID availabilityId) {
        requireProfessional(professionalId);
        ProfessionalAvailability availability = repository.findByIdAndProfessionalId(availabilityId, professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disponibilité introuvable"));
        repository.delete(availability);
    }

    private Map<String, Object> toJson(ProfessionalAvailability availability) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", availability.getId().toString());
        json.put("availability_type", availability.getAvailabilityType());
        json.put("day_of_week", availability.getDayOfWeek());
        json.put("start_time", availability.getStartTime());
        json.put("end_time", availability.getEndTime());
        json.put("start_date", availability.getStartDate() != null ? availability.getStartDate().toString() : null);
        json.put("end_date", availability.getEndDate() != null ? availability.getEndDate().toString() : null);
        json.put("is_recurring", availability.isRecurring());
        json.put("recurrence_pattern", availability.getRecurrencePattern());
        json.put("notes", availability.getNotes());
        return json;
    }

    private void requireProfessional(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        if (user.getRole() != UserRole.pro && user.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux professionnels");
        }
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

    private static LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception ex) {
            return null;
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
