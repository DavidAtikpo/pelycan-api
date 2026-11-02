package com.pelycan.api.web;

import com.pelycan.api.service.HelplineService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/helplines")
public class HelplineController {

    private final HelplineService helplineService;

    public HelplineController(HelplineService helplineService) {
        this.helplineService = helplineService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return helplineService.listActive();
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helplineService.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return helplineService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Map<String, Object> delete(@PathVariable int id) {
        helplineService.delete(id);
        return Map.of("success", true);
    }
}
