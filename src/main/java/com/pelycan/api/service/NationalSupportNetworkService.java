package com.pelycan.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelycan.api.domain.NationalSupportNetwork;
import com.pelycan.api.repository.NationalSupportNetworkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NationalSupportNetworkService {

    private final NationalSupportNetworkRepository repository;
    private final ObjectMapper objectMapper;

    public NationalSupportNetworkService(
            NationalSupportNetworkRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listActive(String section) {
        List<NationalSupportNetwork> networks = section == null || section.isBlank()
                ? repository.findByActiveTrueOrderByOrderIndexAscNameAsc()
                : repository.findByActiveTrueAndSectionOrderByOrderIndexAscNameAsc(section.trim());
        List<Map<String, Object>> data = networks.stream()
                .map(this::toJson)
                .toList();
        return Map.of("success", true, "data", data);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID id) {
        NationalSupportNetwork network = repository.findById(id)
                .filter(NationalSupportNetwork::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réseau non trouvé"));
        return Map.of("success", true, "data", toJson(network));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        NationalSupportNetwork network = new NationalSupportNetwork();
        applyBody(network, body, true);
        repository.save(network);
        return Map.of("success", true, "data", toJson(network));
    }

    @Transactional
    public Map<String, Object> update(UUID id, Map<String, Object> body) {
        NationalSupportNetwork network = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réseau non trouvé"));
        applyBody(network, body, false);
        repository.save(network);
        return Map.of("success", true, "data", toJson(network));
    }

    @Transactional
    public void delete(UUID id) {
        NationalSupportNetwork network = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réseau non trouvé"));
        repository.delete(network);
    }

    private void applyBody(NationalSupportNetwork network, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("name")) {
            network.setName(required(body, "name"));
        }
        if (creating || body.containsKey("subtitle")) {
            network.setSubtitle(string(first(body, "subtitle")));
        }
        if (creating || body.containsKey("description")) {
            network.setDescription(string(first(body, "description")));
        }
        if (creating || body.containsKey("websiteUrl") || body.containsKey("website_url")) {
            network.setWebsiteUrl(string(first(body, "websiteUrl", "website_url")));
        }
        if (creating || body.containsKey("icon")) {
            network.setIcon(stringOrDefault(first(body, "icon"), "volunteer_activism"));
        }
        if (creating || body.containsKey("color")) {
            network.setColor(stringOrDefault(first(body, "color"), "purple"));
        }
        if (creating || body.containsKey("services")) {
            network.setServices(serializeServices(body.get("services")));
        }
        if (creating || body.containsKey("section")) {
            network.setSection(stringOrDefault(first(body, "section"), "national"));
        } else if (creating) {
            network.setSection("national");
        }
        Object order = first(body, "orderIndex", "order_index");
        if (order != null) {
            network.setOrderIndex(toInt(order));
        } else if (creating) {
            network.setOrderIndex(0);
        }
        Object active = first(body, "isActive", "is_active");
        if (active != null) {
            network.setActive(toBoolean(active));
        } else if (creating) {
            network.setActive(true);
        }
    }

    private Map<String, Object> toJson(NationalSupportNetwork network) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", network.getId());
        json.put("name", network.getName());
        json.put("subtitle", network.getSubtitle());
        json.put("description", network.getDescription());
        json.put("website_url", network.getWebsiteUrl());
        json.put("icon", network.getIcon());
        json.put("color", network.getColor());
        json.put("services", parseServices(network.getServices()));
        json.put("section", network.getSection());
        json.put("order_index", network.getOrderIndex());
        json.put("is_active", network.isActive());
        json.put("created_at", network.getCreatedAt());
        json.put("updated_at", network.getUpdatedAt());
        return json;
    }

    private String serializeServices(Object value) {
        if (value == null) {
            return "[]";
        }
        if (value instanceof String text) {
            return text.isBlank() ? "[]" : text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "services invalides");
        }
    }

    private List<String> parseServices(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of(raw);
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
