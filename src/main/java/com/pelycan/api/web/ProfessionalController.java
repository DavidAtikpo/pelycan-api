package com.pelycan.api.web;

import com.pelycan.api.service.SoutienPsychologiqueService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final SoutienPsychologiqueService soutienPsychologiqueService;

    public ProfessionalController(SoutienPsychologiqueService soutienPsychologiqueService) {
        this.soutienPsychologiqueService = soutienPsychologiqueService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean available) {
        Map<String, Object> result = soutienPsychologiqueService.listProfessionals(type);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> professionals =
                (List<Map<String, Object>>) result.getOrDefault("professionals", List.of());

        List<Map<String, Object>> data = professionals.stream()
                .filter(pro -> available == null || available == Boolean.TRUE)
                .map(this::toMobileFormat)
                .toList();

        return ApiResponse.ok(data);
    }

    private Map<String, Object> toMobileFormat(Map<String, Object> pro) {
        String id = String.valueOf(pro.get("id"));
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("firstName", pro.get("first_name"));
        user.put("lastName", pro.get("last_name"));

        Map<String, Object> mobile = new LinkedHashMap<>();
        mobile.put("id", id);
        mobile.put("userId", id);
        mobile.put("user", user);
        mobile.put("title", pro.get("speciality"));
        mobile.put("specialty", pro.get("speciality"));
        mobile.put("available", true);
        return mobile;
    }
}
