package com.pelycan.api.service;

import com.pelycan.api.domain.DemandeAjoutLogement;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.DemandeAjoutLogementRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class DemandeAjoutLogementService {

    private final DemandeAjoutLogementRepository repository;
    private final UserRepository userRepository;

    public DemandeAjoutLogementService(
            DemandeAjoutLogementRepository repository,
            UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllForAdmin() {
        return repository.findAllByOrderByDateCreationDesc().stream()
                .map(this::toAdminJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id, User actor) {
        DemandeAjoutLogement d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        if (actor.getRole() != UserRole.admin
                && (d.getUserId() == null || !d.getUserId().equals(actor.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }
        return toDetailJson(d);
    }

    @Transactional
    public Map<String, Object> create(User user, Map<String, Object> body) {
        String nom = required(body, "nom");
        String prenom = required(body, "prenom");
        String telephone = required(body, "telephone");
        String email = required(body, "email");
        String raisonDemande = required(body, "raisonDemande");
        boolean estProprio = bool(body.get("estProprio"));

        if (user != null && !repository.findActiveByUserId(user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous avez déjà une demande en cours de traitement ou approuvée.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> infos = (Map<String, Object>) body.get("informationsPersonnelles");

        DemandeAjoutLogement d = new DemandeAjoutLogement();
        d.setNom(nom);
        d.setPrenom(prenom);
        d.setTelephone(telephone);
        d.setEmail(email);
        d.setRaisonDemande(raisonDemande);
        d.setEstProprio(estProprio);
        d.setStatut("en_attente");
        d.setUserId(user != null ? user.getId() : null);

        if (infos != null) {
            d.setNumeroPieceIdentite(string(infos.get("numeroPieceIdentite")));
            d.setNumeroDemandeLogement(string(infos.get("numeroDemandeLogement")));
            d.setNumeroDalo(string(infos.get("numeroDalo")));
            d.setNombrePersonnes(string(infos.get("nombrePersonnes")));
            d.setBulletinsSalaire(stringList(infos.get("bulletinsSalaire")));
            d.setContratTravail(stringList(infos.get("contratTravail")));
            d.setQuittancesLoyer(stringList(infos.get("quittancesLoyer")));
            d.setJustificatifPriseEnCharge(stringList(infos.get("justificatifPriseEnCharge")));
            d.setPieceIdentite(stringList(infos.get("pieceIdentite")));
            d.setLivretFamille(stringList(infos.get("livretFamille")));
            d.setNotificationCaf(stringList(infos.get("notificationCaf")));
        }

        repository.save(d);
        return toCreateResponse(d);
    }

    @Transactional
    public Map<String, Object> updateStatut(UUID id, String statut, String raisonRefus) {
        if (statut == null || statut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut est requis");
        }
        if ("refusee".equals(statut) && (raisonRefus == null || raisonRefus.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La raison du refus est requise");
        }
        DemandeAjoutLogement d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        d.setStatut(statut);
        d.setRaisonRefus(raisonRefus);
        repository.save(d);

        if ("approuvee".equals(statut) && d.getUserId() != null) {
            userRepository.findById(d.getUserId()).ifPresent(u -> {
                if (u.getRole() == UserRole.user) {
                    u.setRole(UserRole.bailleur);
                    userRepository.save(u);
                }
            });
        }

        return Map.of(
                "message", "Statut mis à jour avec succès",
                "demande", toDetailJson(d));
    }

    @Transactional
    public void delete(UUID id, User actor) {
        DemandeAjoutLogement d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        assertOwnerOrAdmin(d, actor);
        repository.delete(d);
    }

    @Transactional
    public Map<String, Object> cancel(UUID id, User actor) {
        DemandeAjoutLogement d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        assertOwnerOrAdmin(d, actor);
        if ("approuvee".equals(d.getStatut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'annuler une demande déjà approuvée");
        }
        d.setStatut("refusee");
        repository.save(d);
        return toCreateResponse(d);
    }

    private void assertOwnerOrAdmin(DemandeAjoutLogement d, User actor) {
        if (actor.getRole() == UserRole.admin) {
            return;
        }
        if (d.getUserId() != null && d.getUserId().equals(actor.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
    }

    private Map<String, Object> toAdminJson(DemandeAjoutLogement d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("nom", d.getNom());
        m.put("prenom", d.getPrenom());
        m.put("telephone", d.getTelephone());
        m.put("email", d.getEmail());
        m.put("statut", d.getStatut());
        m.put("dateCreation", d.getDateCreation());
        m.put("dateMiseAJour", d.getDateMiseAJour());
        m.put("raisonDemande", d.getRaisonDemande());
        m.put("estProprio", d.isEstProprio());
        m.put("userId", d.getUserId());
        m.put("raisonRefus", d.getRaisonRefus());
        m.put("numeroPieceIdentite", d.getNumeroPieceIdentite());
        m.put("numeroDemandeLogement", d.getNumeroDemandeLogement());
        m.put("numeroDalo", d.getNumeroDalo());
        m.put("nombrePersonnes", d.getNombrePersonnes());
        m.put("documents", documentsMap(d));
        if (d.getUserId() != null) {
            userRepository.findById(d.getUserId()).ifPresent(u -> {
                m.put("userEmail", u.getEmail());
                m.put("userRole", u.getRole().name());
            });
        }
        return m;
    }

    private Map<String, Object> toCreateResponse(DemandeAjoutLogement d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("nom", d.getNom());
        m.put("prenom", d.getPrenom());
        m.put("telephone", d.getTelephone());
        m.put("email", d.getEmail());
        m.put("statut", d.getStatut());
        m.put("dateCreation", d.getDateCreation());
        m.put("dateMiseAJour", d.getDateMiseAJour());
        m.put("raisonDemande", d.getRaisonDemande());
        m.put("estProprio", d.isEstProprio());
        m.put("userId", d.getUserId());
        m.put("documents", documentsMap(d));
        return m;
    }

    private Map<String, Object> toDetailJson(DemandeAjoutLogement d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("nom", d.getNom());
        m.put("prenom", d.getPrenom());
        m.put("telephone", d.getTelephone());
        m.put("email", d.getEmail());
        m.put("justificatif", d.getJustificatif());
        m.put("statut", d.getStatut());
        m.put("date_creation", d.getDateCreation());
        m.put("date_mise_a_jour", d.getDateMiseAJour());
        m.put("raison_demande", d.getRaisonDemande());
        m.put("est_proprio", d.isEstProprio());
        m.put("user_id", d.getUserId());
        return m;
    }

    private static Map<String, Object> documentsMap(DemandeAjoutLogement d) {
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("bulletinsSalaire", nullToEmpty(d.getBulletinsSalaire()));
        docs.put("contratTravail", nullToEmpty(d.getContratTravail()));
        docs.put("quittancesLoyer", nullToEmpty(d.getQuittancesLoyer()));
        docs.put("justificatifPriseEnCharge", nullToEmpty(d.getJustificatifPriseEnCharge()));
        docs.put("pieceIdentite", nullToEmpty(d.getPieceIdentite()));
        docs.put("livretFamille", nullToEmpty(d.getLivretFamille()));
        docs.put("notificationCaf", nullToEmpty(d.getNotificationCaf()));
        return docs;
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list != null ? list : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(raw));
    }

    private static String required(Map<String, Object> body, String key) {
        String v = string(body.get(key));
        if (v == null || v.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Veuillez fournir tous les champs obligatoires");
        }
        return v;
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(o));
    }
}
