package com.pelycan.api.web;

import com.pelycan.api.domain.DangerAlert;
import com.pelycan.api.domain.User;
import com.pelycan.api.service.DangerAlertService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class DangerAlertController {

    private final DangerAlertService dangerAlertService;

    public DangerAlertController(DangerAlertService dangerAlertService) {
        this.dangerAlertService = dangerAlertService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dangerAlertService.create(user, body, false));
    }

    @PostMapping("/voice-detection")
    public ResponseEntity<Map<String, Object>> voiceDetection(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dangerAlertService.create(user, body, true));
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> list() {
        return dangerAlertService.listAll();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> activeSummary() {
        return dangerAlertService.activeSummary();
    }

    @PostMapping("/{alertId}/message")
    @PreAuthorize("hasRole('admin')")
    public Map<String, String> sendMessage(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        dangerAlertService.addMessage(alertId, user.getId(), String.valueOf(body.get("message")));
        return Map.of("message", "Message envoyé avec succès");
    }

    @PutMapping("/{alertId}/process")
    @PreAuthorize("hasRole('admin')")
    public Map<String, String> process(@PathVariable UUID alertId) {
        dangerAlertService.process(alertId);
        return Map.of("message", "Alerte marquée comme traitée");
    }

    @PatchMapping("/{id}/viewed")
    @PreAuthorize("hasRole('admin')")
    public DangerAlert markViewed(@PathVariable UUID id) {
        return dangerAlertService.markViewed(id);
    }
}
