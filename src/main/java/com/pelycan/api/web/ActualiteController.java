package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.ActualiteService;
import com.pelycan.api.web.dto.ActualiteAdminDto;
import com.pelycan.api.web.dto.ActualitePublicDto;
import com.pelycan.api.web.dto.ActualiteWriteRequest;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/actualites")
public class ActualiteController {

    private final ActualiteService actualiteService;

    public ActualiteController(ActualiteService actualiteService) {
        this.actualiteService = actualiteService;
    }

    @GetMapping
    public ApiResponse<List<ActualitePublicDto>> published(@RequestParam(required = false) String category) {
        return ApiResponse.ok(actualiteService.listPublished(category));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<List<ActualiteAdminDto>> adminList() {
        return ApiResponse.ok(actualiteService.listAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ActualiteAdminDto> create(
            @RequestBody ActualiteWriteRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(actualiteService.create(request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<ActualiteAdminDto> update(
            @PathVariable UUID id,
            @RequestBody ActualiteWriteRequest request,
            @AuthenticationPrincipal User user) {
        return ApiResponse.ok(actualiteService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        actualiteService.delete(id);
        return new ApiResponse<>(true, null, "Actualité supprimée");
    }
}
