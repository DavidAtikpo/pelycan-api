package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.AppointmentManagementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentManagementService appointmentManagementService;

    public AppointmentController(AppointmentManagementService appointmentManagementService) {
        this.appointmentManagementService = appointmentManagementService;
    }

    @GetMapping("/professional/all")
    public Map<String, Object> professionalAppointments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {
        return appointmentManagementService.listForProfessional(user.getId(), status);
    }

    @PatchMapping("/{appointmentId}/accept")
    public Map<String, Object> accept(
            @AuthenticationPrincipal User user,
            @PathVariable UUID appointmentId) {
        appointmentManagementService.accept(user.getId(), appointmentId);
        return Map.of("success", true, "status", "confirmed");
    }

    @PatchMapping("/{appointmentId}/reject")
    public Map<String, Object> reject(
            @AuthenticationPrincipal User user,
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, Object> body) {
        String reason = body.get("rejection_reason") != null
                ? body.get("rejection_reason").toString()
                : "";
        appointmentManagementService.reject(user.getId(), appointmentId, reason);
        return Map.of("success", true, "status", "rejected");
    }

    @PatchMapping("/{appointmentId}/reschedule")
    public Map<String, Object> reschedule(
            @AuthenticationPrincipal User user,
            @PathVariable UUID appointmentId,
            @RequestBody Map<String, Object> body) {
        Object rawDate = body.get("appointment_date") != null
                ? body.get("appointment_date")
                : body.get("new_date");
        Instant newDate = rawDate != null ? Instant.parse(rawDate.toString()) : null;
        appointmentManagementService.reschedule(user.getId(), appointmentId, newDate);
        return Map.of("success", true, "status", "pending");
    }

    @PatchMapping("/{appointmentId}/cancel")
    public Map<String, Object> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable UUID appointmentId) {
        appointmentManagementService.cancel(user.getId(), appointmentId);
        return Map.of("success", true, "status", "cancelled");
    }
}
