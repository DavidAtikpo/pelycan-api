package com.pelycan.api.web;

import com.pelycan.api.domain.UserRole;
import com.pelycan.api.service.AdminCaseService;
import com.pelycan.api.service.DangerAlertService;
import com.pelycan.api.service.LogementService;
import com.pelycan.api.service.UserAlertService;
import com.pelycan.api.service.UserService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final LogementService logementService;
    private final UserAlertService userAlertService;
    private final UserService userService;
    private final AdminCaseService adminCaseService;
    private final DangerAlertService dangerAlertService;

    public AdminController(
            LogementService logementService,
            UserAlertService userAlertService,
            UserService userService,
            AdminCaseService adminCaseService,
            DangerAlertService dangerAlertService) {
        this.logementService = logementService;
        this.userAlertService = userAlertService;
        this.userService = userService;
        this.adminCaseService = adminCaseService;
        this.dangerAlertService = dangerAlertService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.ok(userService.listAllForAdmin());
    }

    @GetMapping("/professionals")
    public List<Map<String, Object>> professionals() {
        return userService.listProfessionalsForAdmin();
    }

    @PatchMapping("/professionals/{id}/status")
    public ApiResponse<Void> updateProfessionalStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        userService.updateStatus(id, string(body.get("status")));
        return ApiResponse.okMessage("Statut du professionnel mis à jour");
    }

    @DeleteMapping("/professionals/{id}")
    public ApiResponse<Void> deleteProfessional(@PathVariable UUID id) {
        userService.deleteProfessional(id);
        return ApiResponse.okMessage("Professionnel supprimé");
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics() {
        return userService.adminStatistics();
    }

    @GetMapping("/cases/unassigned")
    public List<Map<String, Object>> unassignedCases() {
        return adminCaseService.listUnassigned();
    }

    @GetMapping("/cases/assigned")
    public List<Map<String, Object>> assignedCases() {
        return adminCaseService.listAssigned();
    }

    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<Void>> assignCase(@RequestBody Map<String, Object> body) {
        String caseId = string(body.get("caseId"));
        String professionalId = string(body.get("professionalId"));
        if (caseId == null || professionalId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("caseId et professionalId sont requis"));
        }
        adminCaseService.assignCase(caseId, UUID.fromString(professionalId), string(body.get("note")));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.okMessage("Cas assigné avec succès"));
    }

    @GetMapping("/emergency-requests")
    public List<Map<String, Object>> emergencyRequests() {
        return dangerAlertService.listForAdmin();
    }

    @GetMapping("/emergencies")
    public List<Map<String, Object>> pendingEmergencies() {
        return dangerAlertService.listPendingForAdmin();
    }

    @PatchMapping("/emergency/{id}/status")
    public ApiResponse<Void> updateEmergencyStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        dangerAlertService.updateStatus(id, string(body.get("status")));
        return ApiResponse.okMessage("Statut de l'urgence mis à jour");
    }

    @PostMapping("/emergency/{id}/assign")
    public ApiResponse<Void> assignEmergency(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String professionalId = string(body.get("professionalId"));
        if (professionalId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "professionalId requis");
        }
        dangerAlertService.assignProfessional(id, UUID.fromString(professionalId));
        return ApiResponse.okMessage("Urgence assignée");
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        userService.updateStatus(id, string(body.get("status")));
        return ApiResponse.okMessage("Statut mis à jour");
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<Void> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        userService.updateRole(id, UserRole.valueOf(string(body.get("role"))));
        return ApiResponse.okMessage("Rôle mis à jour");
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @GetMapping("/logements")
    public List<Map<String, Object>> logements() {
        return logementService.listAdmin();
    }

    @PatchMapping("/logements/{id}/approve")
    public ApiResponse<Void> approveLogement(@PathVariable UUID id) {
        logementService.approve(id);
        return ApiResponse.okMessage("Logement approuvé");
    }

    @PutMapping("/logements/{id}/disponibilite")
    public ApiResponse<Void> disponibilite(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        boolean d = Boolean.parseBoolean(String.valueOf(body.get("disponibilite")));
        logementService.updateDisponibilite(id, d);
        return ApiResponse.okMessage("Disponibilité mise à jour");
    }

    @DeleteMapping("/logements/{id}")
    public ApiResponse<Void> deleteLogement(@PathVariable UUID id) {
        logementService.delete(id, null);
        return ApiResponse.okMessage("Logement supprimé");
    }

    @GetMapping("/user-alerts/active")
    public ApiResponse<List<Map<String, Object>>> activeAlerts() {
        return ApiResponse.ok(userAlertService.listActiveForAdmin());
    }

    @GetMapping("/user-alerts/list")
    public ApiResponse<List<Map<String, Object>>> alertList(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(userAlertService.listAllForAdmin(limit));
    }

    @PostMapping("/user-alerts/{alertId}/resolve")
    public ApiResponse<Void> resolveAlert(@PathVariable UUID alertId) {
        userAlertService.resolveAdmin(alertId);
        return ApiResponse.okMessage("Alerte marquée comme résolue (intervention faite)");
    }

    @PostMapping("/user-alerts/{alertId}/cancel")
    public ApiResponse<Void> cancelAlert(@PathVariable UUID alertId) {
        userAlertService.cancelAdmin(alertId);
        return ApiResponse.okMessage("Alerte annulée (fausse alerte)");
    }
}
