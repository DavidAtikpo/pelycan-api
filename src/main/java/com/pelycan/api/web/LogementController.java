package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.DemandeService;
import com.pelycan.api.service.LogementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logements")
public class LogementController {

    private final LogementService logementService;
    private final DemandeService demandeService;

    public LogementController(LogementService logementService, DemandeService demandeService) {
        this.logementService = logementService;
        this.demandeService = demandeService;
    }

    @GetMapping
    public List<Map<String, Object>> listPublic() {
        return logementService.listPublic();
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('pro','admin','bailleur')")
    public List<Map<String, Object>> listMine(@AuthenticationPrincipal User user) {
        return logementService.listByOwner(user.getId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable UUID id) {
        return logementService.getById(id);
    }

    @GetMapping("/{id}/demandes")
    @PreAuthorize("hasAnyRole('pro','admin','bailleur')")
    public List<Map<String, Object>> listDemandes(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return demandeService.listByLogement(id, user);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('pro','admin','bailleur')")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logementService.create(user, body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('pro','admin','bailleur')")
    public Map<String, Object> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return logementService.update(id, user, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('pro','admin','bailleur')")
    public Map<String, Object> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        logementService.delete(id, user);
        return Map.of("message", "Logement supprimé avec succès", "id", id);
    }
}
