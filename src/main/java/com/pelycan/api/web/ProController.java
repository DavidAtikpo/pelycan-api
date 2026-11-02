package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.ProWorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pro")
public class ProController {

    private final ProWorkspaceService proWorkspaceService;

    public ProController(ProWorkspaceService proWorkspaceService) {
        this.proWorkspaceService = proWorkspaceService;
    }

    @GetMapping("/cases/active")
    public List<Map<String, Object>> activeCases(@AuthenticationPrincipal User user) {
        return proWorkspaceService.listActiveCases(user.getId());
    }

    @GetMapping("/cases/{caseId}")
    public Map<String, Object> caseDetails(
            @AuthenticationPrincipal User user,
            @PathVariable String caseId) {
        return proWorkspaceService.getCaseDetails(user.getId(), caseId);
    }

    @PatchMapping("/cases/{caseId}/status")
    public Map<String, Object> updateCaseStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String caseId,
            @RequestBody Map<String, Object> body) {
        String status = body.get("status") != null ? body.get("status").toString() : "pending";
        proWorkspaceService.updateCaseStatus(user.getId(), caseId, status);
        return Map.of("success", true, "status", status);
    }

    @PostMapping("/cases/{caseId}/notes")
    public ResponseEntity<Map<String, Object>> addCaseNote(
            @AuthenticationPrincipal User user,
            @PathVariable String caseId,
            @RequestBody Map<String, Object> body) {
        String note = body.get("note") != null ? body.get("note").toString() : "";
        Map<String, Object> created = proWorkspaceService.addCaseNote(user.getId(), caseId, note);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/appointments")
    public List<Map<String, Object>> appointments(@AuthenticationPrincipal User user) {
        return proWorkspaceService.listAppointments(user.getId());
    }

    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> createAppointment(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> created = proWorkspaceService.createAppointment(user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    public Map<String, Object> updateAppointmentStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, Object> body) {
        String status = body.get("status") != null ? body.get("status").toString() : "pending";
        return proWorkspaceService.updateAppointmentStatus(user.getId(), appointmentId, status);
    }
}
