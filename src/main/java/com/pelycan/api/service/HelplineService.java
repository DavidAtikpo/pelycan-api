package com.pelycan.api.service;

import com.pelycan.api.domain.Helpline;
import com.pelycan.api.repository.HelplineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HelplineService {

    private final HelplineRepository helplineRepository;

    public HelplineService(HelplineRepository helplineRepository) {
        this.helplineRepository = helplineRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActive() {
        return helplineRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toJson)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Helpline helpline = new Helpline();
        applyBody(helpline, body, true);
        helplineRepository.save(helpline);
        return toJson(helpline);
    }

    @Transactional
    public Map<String, Object> update(int id, Map<String, Object> body) {
        Helpline helpline = helplineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ligne d'écoute non trouvée"));
        applyBody(helpline, body, false);
        helplineRepository.save(helpline);
        return toJson(helpline);
    }

    @Transactional
    public void delete(int id) {
        Helpline helpline = helplineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ligne d'écoute non trouvée"));
        helplineRepository.delete(helpline);
    }

    private void applyBody(Helpline helpline, Map<String, Object> body, boolean creating) {
        String title = string(body.get("title"));
        String phoneNumber = string(first(body, "phoneNumber", "phone_number"));
        String description = string(body.get("description"));
        String hours = string(body.get("hours"));

        if (creating && (title == null || phoneNumber == null || description == null || hours == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les champs title, phoneNumber, description et hours sont obligatoires");
        }

        if (title != null) {
            helpline.setTitle(title);
        }
        if (phoneNumber != null) {
            helpline.setPhoneNumber(phoneNumber);
        }
        if (description != null) {
            helpline.setDescription(description);
        }
        if (hours != null) {
            helpline.setHours(hours);
        }

        String icon = string(body.get("icon"));
        if (icon != null) {
            helpline.setIcon(icon);
        }

        String color = string(body.get("color"));
        if (color != null) {
            helpline.setColor(color);
        }

        String type = string(body.get("type"));
        if (type != null) {
            helpline.setType(type);
        }

        Object emergency = first(body, "isEmergency", "is_emergency");
        if (emergency != null) {
            helpline.setEmergency(toBoolean(emergency));
        }

        Object order = first(body, "order", "display_order");
        if (order != null) {
            helpline.setDisplayOrder(toInt(order));
        } else if (creating) {
            helpline.setDisplayOrder(0);
        }

        Object active = first(body, "isActive", "is_active");
        if (active != null) {
            helpline.setActive(toBoolean(active));
        }
    }

    private Map<String, Object> toJson(Helpline helpline) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", helpline.getId());
        json.put("title", helpline.getTitle());
        json.put("phoneNumber", helpline.getPhoneNumber());
        json.put("description", helpline.getDescription());
        json.put("hours", helpline.getHours());
        json.put("icon", helpline.getIcon());
        json.put("color", helpline.getColor());
        json.put("type", helpline.getType());
        json.put("isEmergency", helpline.isEmergency());
        json.put("order", helpline.getDisplayOrder());
        json.put("isActive", helpline.isActive());
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

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
