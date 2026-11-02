package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.DemandeAjoutLogementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes-ajout-logement")
public class DemandeAjoutLogementController {

    private final DemandeAjoutLogementService service;

    public DemandeAjoutLogementController(DemandeAjoutLogementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public List<Map<String, Object>> listAll() {
        return service.listAllForAdmin();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.getById(id, user);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, body));
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> updateStatut(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return service.updateStatut(
                id,
                string(body.get("statut")),
                string(body.get("raisonRefus")));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        service.delete(id, user);
        return Map.of("message", "Demande supprimée avec succès", "id", id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.cancel(id, user);
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
