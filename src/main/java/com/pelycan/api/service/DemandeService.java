package com.pelycan.api.service;

import com.pelycan.api.domain.Demande;
import com.pelycan.api.domain.Logement;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.DemandeRepository;
import com.pelycan.api.repository.LogementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final LogementRepository logementRepository;

    public DemandeService(DemandeRepository demandeRepository, LogementRepository logementRepository) {
        this.demandeRepository = demandeRepository;
        this.logementRepository = logementRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAllWrapped() {
        List<Map<String, Object>> data = demandeRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toJson)
                .toList();
        return Map.of("success", true, "data", data);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByType(String type) {
        if (!type.equals("logement") && !type.equals("ht")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de demande invalide");
        }
        return demandeRepository.findAll().stream()
                .filter(d -> type.equals(d.getType()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id) {
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        return toJson(d);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByUser(UUID userId) {
        return demandeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByLogement(UUID logementId, User actor) {
        Logement logement = logementRepository.findById(logementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé"));
        assertCanViewLogementDemandes(logement, actor);
        return demandeRepository.findByLogementIdOrderByCreatedAtDesc(logementId).stream()
                .map(this::toJson)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(User user, Map<String, Object> body) {
        UUID logementId = uuid(body.get("logementId"));
        if (logementId != null && logementRepository.findById(logementId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logement non trouvé");
        }
        Demande d = new Demande();
        d.setNom(required(body, "nom"));
        d.setPrenom(required(body, "prenom"));
        d.setTelephone(required(body, "telephone"));
        d.setEmail(required(body, "email"));
        d.setNombrePersonnes(required(body, "nombrePersonnes"));
        d.setNiveauUrgence(stringOr(body.get("niveauUrgence"), "normal"));
        d.setMessage(string(body.get("message")));
        d.setLogementId(logementId);
        d.setCentreType(string(body.get("centreType")));
        d.setType(required(body, "type"));
        d.setStatus("en_attente");
        d.setUserId(user != null ? user.getId() : null);
        demandeRepository.save(d);
        return Map.of(
                "success", true,
                "message", "Demande créée avec succès",
                "data", toJson(d));
    }

    @Transactional
    public Map<String, Object> updateStatus(UUID id, User actor, String status) {
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        if (actor.getRole() == UserRole.bailleur && d.getLogementId() != null) {
            Logement l = logementRepository.findById(d.getLogementId()).orElse(null);
            if (l == null || !l.getProprietaireId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vous n'êtes pas autorisé à modifier cette demande");
            }
        }
        d.setStatus(status);
        demandeRepository.save(d);
        return Map.of(
                "success", true,
                "message", "Statut de la demande mis à jour avec succès",
                "data", toJson(d));
    }

    @Transactional
    public void delete(UUID id) {
        if (!demandeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée");
        }
        demandeRepository.deleteById(id);
    }

    private void assertCanViewLogementDemandes(Logement logement, User actor) {
        if (actor.getRole() == UserRole.admin || actor.getRole() == UserRole.pro) {
            return;
        }
        if (actor.getRole() == UserRole.bailleur && logement.getProprietaireId().equals(actor.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
    }

    private Map<String, Object> toJson(Demande d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("nom", d.getNom());
        m.put("prenom", d.getPrenom());
        m.put("telephone", d.getTelephone());
        m.put("email", d.getEmail());
        m.put("nombrePersonnes", d.getNombrePersonnes());
        m.put("niveauUrgence", d.getNiveauUrgence());
        m.put("message", d.getMessage());
        m.put("logementId", d.getLogementId());
        m.put("userId", d.getUserId());
        m.put("centreType", d.getCentreType());
        m.put("type", d.getType());
        m.put("status", d.getStatus());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        if (d.getLogementId() != null) {
            logementRepository.findById(d.getLogementId()).ifPresent(l -> m.put("logement", Map.of(
                    "id", l.getId(),
                    "titre", l.getTitre(),
                    "adresse", l.getAdresse(),
                    "ville", l.getVille())));
        }
        return m;
    }

    private static UUID uuid(Object o) {
        if (o == null || String.valueOf(o).isBlank()) {
            return null;
        }
        return UUID.fromString(String.valueOf(o));
    }

    private static String required(Map<String, Object> body, String key) {
        String v = string(body.get(key));
        if (v == null || v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Champ requis: " + key);
        }
        return v;
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String stringOr(Object o, String def) {
        String s = string(o);
        return s == null || s.isBlank() ? def : s;
    }
}
