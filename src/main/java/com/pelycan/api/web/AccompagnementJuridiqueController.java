package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.AccompagnementJuridiqueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accompagnement-juridique")
public class AccompagnementJuridiqueController {

    private final AccompagnementJuridiqueService service;

    public AccompagnementJuridiqueController(AccompagnementJuridiqueService service) {
        this.service = service;
    }

    @GetMapping("/consultations")
    public Map<String, Object> list(@AuthenticationPrincipal User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("consultations", service.listForUser(user.getId()));
        return response;
    }

    @GetMapping("/consultations/{id}")
    public Map<String, Object> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("consultation", service.getForUser(user.getId(), id));
        return response;
    }

    @PostMapping("/demandes")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Demande créée avec succès");
        response.put("consultation", service.create(user.getId(), body));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/consultations/{id}")
    public Map<String, Object> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        service.deleteForUser(user.getId(), id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Consultation supprimée");
        return response;
    }
}
