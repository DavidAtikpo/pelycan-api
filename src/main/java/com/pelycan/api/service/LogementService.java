package com.pelycan.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pelycan.api.domain.Logement;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.LogementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class LogementService {

    private final LogementRepository logementRepository;
    private final ObjectMapper objectMapper;

    public LogementService(LogementRepository logementRepository, ObjectMapper objectMapper) {
        this.logementRepository = logementRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublic() {
        return logementRepository.findByStatusOrderByCreatedAtDesc("disponible").stream()
                .map(this::toPublicJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id) {
        Logement l = logementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        return toDetailJson(l);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByOwner(UUID userId) {
        return logementRepository.findByProprietaireIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toPublicJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAdmin() {
        return logementRepository.findAllWithProprietaire().stream()
                .map(this::toAdminJson)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(User owner, Map<String, Object> body) {
        Logement l = new Logement();
        l.setProprietaireId(owner.getId());
        applyBody(l, body, true);
        l.setStatus(body.containsKey("status") ? String.valueOf(body.get("status")) : "disponible");
        logementRepository.save(l);
        return Map.of("message", "Logement ajouté avec succès", "id", l.getId());
    }

    @Transactional
    public Map<String, Object> update(UUID id, User actor, Map<String, Object> body) {
        Logement l = logementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        assertCanModify(l, actor);
        applyBody(l, body, false);
        logementRepository.save(l);
        return Map.of("message", "Logement mis à jour", "id", l.getId());
    }

    @Transactional
    public void delete(UUID id, User actor) {
        Logement l = logementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        if (actor != null) {
            assertCanModify(l, actor);
        }
        logementRepository.delete(l);
    }

    @Transactional
    public void approve(UUID id) {
        Logement l = logementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        l.setStatus("disponible");
        logementRepository.save(l);
    }

    @Transactional
    public void updateDisponibilite(UUID id, boolean disponibilite) {
        Logement l = logementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        l.setDisponibilite(disponibilite);
        logementRepository.save(l);
    }

    private void assertCanModify(Logement l, User actor) {
        if (actor.getRole() == UserRole.admin) {
            return;
        }
        if (actor.getRole() == UserRole.pro || actor.getRole() == UserRole.bailleur) {
            if (!l.getProprietaireId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez modifier que vos propres logements.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyBody(Logement l, Map<String, Object> body, boolean create) {
        if (body.get("titre") != null) {
            l.setTitre(String.valueOf(body.get("titre")));
        }
        if (body.get("description") != null) {
            l.setDescription(String.valueOf(body.get("description")));
        }
        if (body.get("adresse") != null) {
            l.setAdresse(String.valueOf(body.get("adresse")));
        }
        if (body.get("ville") != null) {
            l.setVille(String.valueOf(body.get("ville")));
        }
        if (body.get("code_postal") != null || body.get("codePostal") != null) {
            Object cp = body.get("code_postal") != null ? body.get("code_postal") : body.get("codePostal");
            l.setCodePostal(String.valueOf(cp));
        }
        if (body.get("type") != null) {
            l.setType(String.valueOf(body.get("type")));
        }
        if (body.get("capacite") != null) {
            l.setCapacite(intVal(body.get("capacite")));
        }
        if (body.get("surface") != null) {
            l.setSurface(intVal(body.get("surface")));
        }
        if (body.get("prix") != null) {
            l.setPrix(intVal(body.get("prix")));
        }
        if (body.get("telephone") != null) {
            l.setTelephone(String.valueOf(body.get("telephone")));
        }
        if (body.get("email") != null) {
            l.setEmail(String.valueOf(body.get("email")));
        }
        if (body.get("niveau_securite") != null) {
            l.setNiveauSecurite(String.valueOf(body.get("niveau_securite")));
        }
        if (body.get("equipements") != null) {
            l.setEquipements(objectMapper.convertValue(body.get("equipements"), Map.class));
        }
        if (body.get("photos") instanceof List<?> list) {
            l.setPhotos(list.stream().map(String::valueOf).toArray(String[]::new));
        }
        if (body.get("type_hebergement") != null) {
            l.setTypeHebergement(String.valueOf(body.get("type_hebergement")));
        }
        if (body.get("latitude") != null) {
            l.setLatitude(new BigDecimal(String.valueOf(body.get("latitude"))));
        }
        if (body.get("longitude") != null) {
            l.setLongitude(new BigDecimal(String.valueOf(body.get("longitude"))));
        }
        if (create && l.getTitre() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titre requis");
        }
    }

    private static int intVal(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(o));
    }

    private Map<String, Object> toPublicJson(Logement l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("titre", l.getTitre());
        m.put("description", l.getDescription() != null ? l.getDescription() : "");
        m.put("adresse", l.getAdresse());
        m.put("codePostal", l.getCodePostal());
        m.put("ville", l.getVille());
        m.put("type", l.getType());
        m.put("capacite", l.getCapacite());
        m.put("surface", l.getSurface());
        m.put("prix", l.getPrix() != null ? l.getPrix() : 0);
        m.put("telephone", l.getTelephone() != null ? l.getTelephone() : "");
        m.put("email", l.getEmail() != null ? l.getEmail() : "");
        m.put("niveau_securite", l.getNiveauSecurite());
        m.put("equipements", l.getEquipements() != null ? l.getEquipements() : Map.of());
        m.put("disponibilite", l.getDisponibilite());
        m.put("status", l.getStatus());
        m.put("photos", l.getPhotos() != null ? Arrays.asList(l.getPhotos()) : List.of());
        m.put("type_hebergement", l.getTypeHebergement());
        m.put("date_debut", l.getDateDebut());
        m.put("date_fin", l.getDateFin());
        m.put("conditions_temporaire", l.getConditionsTemporaire());
        m.put("latitude", l.getLatitude() != null ? l.getLatitude().doubleValue() : null);
        m.put("longitude", l.getLongitude() != null ? l.getLongitude().doubleValue() : null);
        m.put("created_at", l.getCreatedAt());
        m.put("updated_at", l.getUpdatedAt());
        m.put("proprietaireId", l.getProprietaireId());
        return m;
    }

    private Map<String, Object> toDetailJson(Logement l) {
        Map<String, Object> m = toPublicJson(l);
        m.put("dateCreation", l.getCreatedAt());
        m.put("dateModification", l.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toAdminJson(Logement l) {
        Map<String, Object> m = toPublicJson(l);
        m.put("typeHebergement", l.getTypeHebergement());
        m.put("dateDebut", l.getDateDebut());
        m.put("dateFin", l.getDateFin());
        m.put("conditionsTemporaire", l.getConditionsTemporaire());
        if (l.getProprietaire() != null) {
            m.put("proprietaire", Map.of(
                    "id", l.getProprietaire().getId(),
                    "first_name", l.getProprietaire().getFirstName(),
                    "last_name", l.getProprietaire().getLastName(),
                    "email", l.getProprietaire().getEmail(),
                    "phone_number", l.getProprietaire().getPhoneNumber()));
        }
        return m;
    }
}
