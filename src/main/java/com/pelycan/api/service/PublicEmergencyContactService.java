package com.pelycan.api.service;

import com.pelycan.api.domain.PublicEmergencyContact;
import com.pelycan.api.repository.PublicEmergencyContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PublicEmergencyContactService {

    private final PublicEmergencyContactRepository repository;

    public PublicEmergencyContactService(PublicEmergencyContactRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listActive() {
        List<Map<String, Object>> data = repository.findByActiveTrueOrderByOrderIndexAscNameAsc().stream()
                .map(this::toJson)
                .toList();
        return Map.of("success", true, "data", data);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID id) {
        PublicEmergencyContact contact = repository.findById(id)
                .filter(PublicEmergencyContact::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact non trouvé"));
        return Map.of("success", true, "data", toJson(contact));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        PublicEmergencyContact contact = new PublicEmergencyContact();
        applyBody(contact, body, true);
        repository.save(contact);
        return Map.of("success", true, "data", toJson(contact));
    }

    @Transactional
    public Map<String, Object> update(UUID id, Map<String, Object> body) {
        PublicEmergencyContact contact = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact non trouvé"));
        applyBody(contact, body, false);
        repository.save(contact);
        return Map.of("success", true, "data", toJson(contact));
    }

    @Transactional
    public void delete(UUID id) {
        PublicEmergencyContact contact = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact non trouvé"));
        repository.delete(contact);
    }

    private void applyBody(PublicEmergencyContact contact, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("name")) {
            contact.setName(required(body, "name"));
        }
        if (creating || body.containsKey("phone")) {
            contact.setPhone(required(body, "phone"));
        }
        if (creating || body.containsKey("description")) {
            contact.setDescription(string(first(body, "description")));
        }
        if (creating || body.containsKey("category")) {
            contact.setCategory(stringOrDefault(first(body, "category"), "other"));
        }
        if (creating || body.containsKey("type")) {
            contact.setType(stringOrDefault(first(body, "type"), "emergency"));
        }
        Object order = first(body, "orderIndex", "order_index");
        if (order != null) {
            contact.setOrderIndex(toInt(order));
        } else if (creating) {
            contact.setOrderIndex(0);
        }
        Object active = first(body, "isActive", "is_active");
        if (active != null) {
            contact.setActive(toBoolean(active));
        } else if (creating) {
            contact.setActive(true);
        }
    }

    private Map<String, Object> toJson(PublicEmergencyContact contact) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", contact.getId());
        json.put("name", contact.getName());
        json.put("phone", contact.getPhone());
        json.put("description", contact.getDescription());
        json.put("category", contact.getCategory());
        json.put("type", contact.getType());
        json.put("order_index", contact.getOrderIndex());
        json.put("is_active", contact.isActive());
        json.put("created_at", contact.getCreatedAt());
        json.put("updated_at", contact.getUpdatedAt());
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

    private static String required(Map<String, Object> body, String key) {
        String value = string(body.get(key));
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ requis: " + key);
        }
        return value;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        String parsed = string(value);
        return parsed == null || parsed.isBlank() ? defaultValue : parsed;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order_index invalide");
        }
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
