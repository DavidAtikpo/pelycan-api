package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.DemandeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService demandeService;

    public DemandeController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','pro','user')")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeService.create(user, body));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','pro')")
    public Map<String, Object> listAll() {
        return demandeService.listAllWrapped();
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> listByUser(@PathVariable UUID userId) {
        return demandeService.listByUser(userId);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('admin','pro')")
    public List<Map<String, Object>> listByType(@PathVariable String type) {
        return demandeService.listByType(type);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','pro')")
    public Map<String, Object> getById(@PathVariable UUID id) {
        return demandeService.getById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin','pro','bailleur')")
    public Map<String, Object> updateStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        String status = String.valueOf(body.get("status"));
        return demandeService.updateStatus(id, user, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, String> delete(@PathVariable UUID id) {
        demandeService.delete(id);
        return Map.of("message", "Demande supprimée avec succès");
    }
}
