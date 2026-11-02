package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.SoutienPsychologiqueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/soutien-psychologique")
public class SoutienPsychologiqueController {

    private final SoutienPsychologiqueService soutienPsychologiqueService;

    public SoutienPsychologiqueController(SoutienPsychologiqueService soutienPsychologiqueService) {
        this.soutienPsychologiqueService = soutienPsychologiqueService;
    }

    @GetMapping("/professionals")
    public Map<String, Object> listProfessionals(@RequestParam(required = false) String type) {
        return soutienPsychologiqueService.listProfessionals(type);
    }

    @GetMapping("/professionals/{id}")
    public Map<String, Object> getProfessional(@PathVariable String id) {
        return soutienPsychologiqueService.getProfessional(id);
    }

    @PostMapping("/moods")
    public Map<String, Object> saveMood(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return soutienPsychologiqueService.saveMood(user.getId(), body);
    }

    @PostMapping("/consultation")
    public Map<String, Object> requestConsultation(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return soutienPsychologiqueService.requestConsultation(user.getId(), body);
    }
}
