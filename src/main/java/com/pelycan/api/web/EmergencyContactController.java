package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.EmergencyContactService;
import com.pelycan.api.service.PublicEmergencyContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;
    private final PublicEmergencyContactService publicEmergencyContactService;

    public EmergencyContactController(
            EmergencyContactService emergencyContactService,
            PublicEmergencyContactService publicEmergencyContactService) {
        this.emergencyContactService = emergencyContactService;
        this.publicEmergencyContactService = publicEmergencyContactService;
    }

    @GetMapping("/public")
    public Map<String, Object> listPublic() {
        return publicEmergencyContactService.listActive();
    }

    @GetMapping("/public/{id}")
    public Map<String, Object> getPublic(@PathVariable UUID id) {
        return publicEmergencyContactService.get(id);
    }

    @PostMapping("/public")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> createPublic(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicEmergencyContactService.create(body));
    }

    @PutMapping("/public/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> updatePublic(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return publicEmergencyContactService.update(id, body);
    }

    @DeleteMapping("/public/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> deletePublic(@PathVariable UUID id) {
        publicEmergencyContactService.delete(id);
        return Map.of("success", true, "message", "Contact supprimé");
    }

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal User user) {
        return emergencyContactService.list(user.getId());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return emergencyContactService.get(user.getId(), id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyContactService.create(user.getId(), body));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return emergencyContactService.update(user.getId(), id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        emergencyContactService.delete(user.getId(), id);
        return Map.of("success", true, "message", "Contact supprimé");
    }
}
