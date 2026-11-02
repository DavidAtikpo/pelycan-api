package com.pelycan.api.service;

import com.pelycan.api.domain.EmergencyContact;
import com.pelycan.api.repository.EmergencyContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmergencyContactService {

    private static final int MAX_CONTACTS = 5;

    private final EmergencyContactRepository repository;

    public EmergencyContactService(EmergencyContactRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(UUID userId) {
        List<Map<String, Object>> data = repository.findByUserId(userId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toJson)
                .toList();
        return Map.of(
                "success", true,
                "data", data,
                "count", data.size(),
                "maxAllowed", MAX_CONTACTS);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID userId, UUID id) {
        EmergencyContact c = findOwned(userId, id);
        return Map.of("success", true, "data", toJson(c));
    }

    @Transactional
    public Map<String, Object> create(UUID userId, Map<String, Object> body) {
        String name = required(body, "name");
        String phone = required(body, "phone");
        long count = repository.findByUserId(userId).size();
        if (count >= MAX_CONTACTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous avez atteint la limite de 5 contacts d'urgence. Supprimez un contact existant avant d'en ajouter un nouveau.");
        }
        EmergencyContact c = new EmergencyContact();
        c.setUserId(userId);
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(string(body.get("email")));
        c.setRelation(string(body.get("relation")));
        repository.save(c);
        return Map.of(
                "success", true,
                "message", "Contact d'urgence ajouté avec succès",
                "data", toJson(c));
    }

    @Transactional
    public Map<String, Object> update(UUID userId, UUID id, Map<String, Object> body) {
        EmergencyContact c = findOwned(userId, id);
        if (body.containsKey("name")) {
            c.setName(required(body, "name"));
        }
        if (body.containsKey("phone")) {
            c.setPhone(required(body, "phone"));
        }
        if (body.containsKey("email")) {
            c.setEmail(string(body.get("email")));
        }
        if (body.containsKey("relation")) {
            c.setRelation(string(body.get("relation")));
        }
        repository.save(c);
        return Map.of(
                "success", true,
                "message", "Contact mis à jour",
                "data", toJson(c));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        EmergencyContact c = findOwned(userId, id);
        repository.delete(c);
    }

    private EmergencyContact findOwned(UUID userId, UUID id) {
        return repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact non trouvé"));
    }

    private Map<String, Object> toJson(EmergencyContact c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("user_id", c.getUserId());
        m.put("name", c.getName());
        m.put("phone", c.getPhone());
        m.put("email", c.getEmail());
        m.put("relation", c.getRelation());
        m.put("created_at", c.getCreatedAt());
        m.put("updated_at", c.getUpdatedAt());
        return m;
    }

    private static String required(Map<String, Object> body, String key) {
        String v = string(body.get(key));
        if (v == null || v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom et le numéro de téléphone sont requis");
        }
        return v;
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
