package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.RelationshipEvaluationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/relationship-evaluations")
public class RelationshipEvaluationController {

    private final RelationshipEvaluationService relationshipEvaluationService;

    public RelationshipEvaluationController(RelationshipEvaluationService relationshipEvaluationService) {
        this.relationshipEvaluationService = relationshipEvaluationService;
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal User user) {
        return relationshipEvaluationService.listForUser(user.getId());
    }

    @GetMapping("/stats/summary")
    public Map<String, Object> stats(@AuthenticationPrincipal User user) {
        return relationshipEvaluationService.statsForUser(user.getId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return relationshipEvaluationService.getForUser(user.getId(), id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> evaluation = relationshipEvaluationService.create(user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("evaluation", evaluation));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> evaluation =
                relationshipEvaluationService.update(user.getId(), id, body);
        return Map.of("evaluation", evaluation);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        relationshipEvaluationService.deleteForUser(user.getId(), id);
        return Map.of("success", true, "message", "Évaluation supprimée");
    }

    @DeleteMapping("/relation/{relationName}")
    public Map<String, Object> deleteByRelationName(
            @PathVariable String relationName,
            @AuthenticationPrincipal User user) {
        String decodedName = URLDecoder.decode(relationName, StandardCharsets.UTF_8);
        relationshipEvaluationService.deleteByRelationName(user.getId(), decodedName);
        return Map.of("success", true, "message", "Évaluations supprimées");
    }
}
