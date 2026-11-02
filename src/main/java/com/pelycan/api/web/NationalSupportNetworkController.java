package com.pelycan.api.web;

import com.pelycan.api.service.NationalSupportNetworkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/national-support-networks")
public class NationalSupportNetworkController {

    private final NationalSupportNetworkService service;

    public NationalSupportNetworkController(NationalSupportNetworkService service) {
        this.service = service;
    }

    @GetMapping("/public")
    public Map<String, Object> listPublic(
            @RequestParam(required = false) String section) {
        return service.listActive(section);
    }

    @GetMapping("/public/{id}")
    public Map<String, Object> getPublic(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/public")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> createPublic(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/public/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> updatePublic(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return service.update(id, body);
    }

    @DeleteMapping("/public/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> deletePublic(@PathVariable UUID id) {
        service.delete(id);
        return Map.of("success", true, "message", "Réseau supprimé");
    }
}
