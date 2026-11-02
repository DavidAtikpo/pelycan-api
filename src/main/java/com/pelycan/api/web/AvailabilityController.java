package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.AvailabilityManagementService;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityController {

    private final AvailabilityManagementService availabilityManagementService;

    public AvailabilityController(AvailabilityManagementService availabilityManagementService) {
        this.availabilityManagementService = availabilityManagementService;
    }

    @GetMapping
    public Map<String, Object> list(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "availability_type", required = false) String availabilityType) {
        return availabilityManagementService.list(user.getId(), availabilityType);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> created = availabilityManagementService.create(user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{availabilityId}")
    public Map<String, Object> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID availabilityId) {
        availabilityManagementService.delete(user.getId(), availabilityId);
        return Map.of("success", true);
    }
}
