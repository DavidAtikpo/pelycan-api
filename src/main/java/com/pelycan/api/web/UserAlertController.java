package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserMedicalInfo;
import com.pelycan.api.service.UserAlertService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-alerts")
public class UserAlertController {

    private final UserAlertService userAlertService;

    public UserAlertController(UserAlertService userAlertService) {
        this.userAlertService = userAlertService;
    }

    @GetMapping("/active/count")
    public ApiResponse<Map<String, Long>> activeCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(Map.of("count", userAlertService.countActiveForUser(user.getId())));
    }

    @GetMapping("/medical-info")
    public ApiResponse<Map<String, Object>> getMedicalInfo(@AuthenticationPrincipal User user) {
        UserMedicalInfo info = userAlertService.getOrCreateMedicalInfo(user.getId());
        return ApiResponse.ok(medicalToJson(info));
    }

    @PutMapping("/medical-info")
    public ApiResponse<Map<String, Object>> updateMedicalInfo(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        UserMedicalInfo info = userAlertService.updateMedicalInfo(user.getId(), body);
        return new ApiResponse<>(true, medicalToJson(info), "Informations médicales mises à jour");
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(userAlertService.listForUser(user.getId(), status, limit));
    }

    @PostMapping("/trigger-admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAdmin(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> data = userAlertService.triggerAdmin(
                user,
                string(body.get("message")),
                dbl(body.get("latitude")),
                dbl(body.get("longitude")));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, data, "Alerte envoyée à l'administration avec votre position."));
    }

    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trigger(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.get("contactIds");
        List<UUID> contactIds = rawIds != null ? rawIds.stream().map(UUID::fromString).toList() : List.of();
        Map<String, Object> data = userAlertService.trigger(
                user,
                string(body.get("message")),
                dbl(body.get("latitude")),
                dbl(body.get("longitude")),
                contactIds);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data));
    }

    @GetMapping("/{alertId}/listening-token")
    public ApiResponse<Map<String, Object>> listeningToken(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(userAlertService.listeningToken(alertId, user));
    }

    @PutMapping("/{alertId}/location")
    public ApiResponse<Map<String, Object>> updateLocation(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> data = userAlertService.updateLocation(
                alertId, user.getId(), dbl(body.get("latitude")), dbl(body.get("longitude")));
        return new ApiResponse<>(true, data, "Position mise à jour");
    }

    @PostMapping("/{alertId}/resolve")
    public ApiResponse<Void> resolve(@PathVariable UUID alertId, @AuthenticationPrincipal User user) {
        userAlertService.resolveOwn(alertId, user);
        return ApiResponse.okMessage("Alerte résolue - Vous êtes en sécurité");
    }

    @PostMapping("/{alertId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable UUID alertId, @AuthenticationPrincipal User user) {
        userAlertService.cancelOwn(alertId, user.getId());
        return ApiResponse.okMessage("Alerte annulée");
    }

    private static Map<String, Object> medicalToJson(UserMedicalInfo info) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user_id", info.getUserId());
        m.put("blood_type", info.getBloodType());
        m.put("allergies", info.getAllergies());
        m.put("medical_conditions", info.getMedicalConditions());
        m.put("emergency_notes", info.getEmergencyNotes());
        m.put("updated_at", info.getUpdatedAt());
        return m;
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Double dbl(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(o));
    }
}
