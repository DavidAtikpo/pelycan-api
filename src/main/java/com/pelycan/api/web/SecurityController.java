package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.UserAlertService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final UserAlertService userAlertService;

    public SecurityController(UserAlertService userAlertService) {
        this.userAlertService = userAlertService;
    }

    @PostMapping("/alert-contacts")
    public ApiResponse<Map<String, Object>> alertContacts(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> raw = (List<String>) body.get("contacts");
        List<UUID> contacts = raw != null ? raw.stream().map(UUID::fromString).toList() : List.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) body.get("location");
        Map<String, Object> data = userAlertService.alertContacts(
                user,
                contacts,
                body.get("message") != null ? String.valueOf(body.get("message")) : null,
                location);
        return ApiResponse.ok(data, "Alertes envoyées aux contacts");
    }

    @GetMapping("/user-alerts")
    public ApiResponse<List<Map<String, Object>>> getUserAlerts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(userAlertService.listForUser(user.getId(), status, 100));
    }

    @PatchMapping("/user-alerts/{alertId}/resolve")
    public ApiResponse<Map<String, Object>> resolve(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(userAlertService.resolveForSecurity(alertId, user.getId()), "Alerte résolue avec succès");
    }

    @PatchMapping("/user-alerts/{alertId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(userAlertService.cancelForSecurity(alertId, user.getId()), "Alerte annulée avec succès");
    }

    @PostMapping("/audio/start")
    public Map<String, Object> startAudio(@AuthenticationPrincipal User user) {
        return Map.of(
                "success", true,
                "recordingId", UUID.randomUUID().toString(),
                "message", "Enregistrement démarré",
                "timestamp", Instant.now());
    }

    @PostMapping("/audio/save")
    public Map<String, Object> saveAudio(@RequestBody Map<String, Object> body) {
        return Map.of(
                "success", true,
                "message", "Enregistrement sauvegardé avec succès",
                "data", Map.of(
                        "recordingId", body.get("recordingId"),
                        "duration", body.getOrDefault("duration", 0),
                        "mimeType", body.getOrDefault("mimeType", "audio/webm"),
                        "savedAt", Instant.now()));
    }

    @PostMapping("/emergency")
    public Map<String, Object> contactEmergency(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return Map.of(
                "success", true,
                "message", "Demande d'urgence créée avec succès",
                "data", Map.of(
                        "emergencyId", UUID.randomUUID().toString(),
                        "status", "pending"));
    }

    @PostMapping("/location/start")
    public Map<String, Object> startLocationSharing() {
        return Map.of(
                "success", true,
                "sharingId", UUID.randomUUID().toString(),
                "message", "Partage de localisation démarré",
                "timestamp", Instant.now());
    }

    @PostMapping("/location/update")
    public Map<String, Object> updateLocationSharing(@RequestBody Map<String, Object> body) {
        return Map.of(
                "success", true,
                "message", "Localisation mise à jour",
                "data", Map.of(
                        "latitude", body.get("latitude"),
                        "longitude", body.get("longitude"),
                        "timestamp", Instant.now()));
    }
}
