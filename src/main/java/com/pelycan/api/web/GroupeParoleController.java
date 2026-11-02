package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.GroupeParoleSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/groupes-parole")
public class GroupeParoleController {

    private final GroupeParoleSessionService sessionService;

    public GroupeParoleController(GroupeParoleSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/sessions")
    public Map<String, Object> listSessions(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        List<Map<String, Object>> sessions = sessionService.listAll(user);
        return Map.of("success", true, "sessions", sessions);
    }

    @GetMapping("/sessions/{id}")
    public Map<String, Object> getSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return Map.of("success", true, "session", sessionService.getById(id, user));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> session = sessionService.create(user, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "session", session));
    }

    @PutMapping("/sessions/{id}")
    public Map<String, Object> updateSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return Map.of("success", true, "session", sessionService.update(id, user, body));
    }

    @PostMapping("/sessions/{id}/inscription")
    public Map<String, Object> enroll(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        sessionService.enroll(id, user);
        return Map.of("success", true, "message", "Inscription réussie");
    }

    @DeleteMapping("/sessions/{id}/desinscription")
    public Map<String, Object> unenroll(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        sessionService.unenroll(id, user);
        return Map.of("success", true, "message", "Désinscription réussie");
    }

    @DeleteMapping("/sessions/{id}")
    public Map<String, Object> deleteSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        sessionService.delete(id, user);
        return Map.of("success", true, "message", "Session supprimée");
    }

    @PostMapping("/sessions/{id}/reschedule")
    public Map<String, Object> rescheduleSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return sessionService.reschedule(id, user, body);
    }
}
