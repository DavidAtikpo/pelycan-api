package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.EvidenceService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceCompatibilityController {

    private final EvidenceService evidenceService;

    public EvidenceCompatibilityController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<java.util.List<Map<String, Object>>> listForUser(
            @AuthenticationPrincipal User user,
            @PathVariable UUID userId,
            @RequestParam(required = false) String type) {
        assertOwner(user, userId);
        return ApiResponse.ok(evidenceService.listForUser(userId, type));
    }

    @GetMapping("/{evidenceId}")
    public ApiResponse<Map<String, Object>> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID evidenceId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return ApiResponse.ok(evidenceService.getForUser(user.getId(), evidenceId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        Map<String, Object> created = evidenceService.create(user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Preuve créée avec succès"));
    }

    @DeleteMapping("/{evidenceId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID evidenceId,
            @RequestBody(required = false) Map<String, Object> body) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        if (body != null && body.get("userId") != null) {
            UUID requestedUserId = UUID.fromString(body.get("userId").toString());
            assertOwner(user, requestedUserId);
        }
        evidenceService.deleteForUser(user.getId(), evidenceId);
        return ApiResponse.okMessage("Preuve supprimée");
    }

    private static void assertOwner(User user, UUID userId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        if (!user.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
    }
}
