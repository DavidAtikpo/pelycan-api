package com.pelycan.api.web;

import com.pelycan.api.service.StorageService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    public record PresignRequest(String folder, String contentType, String extension) {}

    @PostMapping("/presign")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> presign(@RequestBody PresignRequest request) {
        return ApiResponse.ok(storageService.createUploadUrl(
                request.folder(),
                request.contentType(),
                request.extension()));
    }

    @PostMapping(value = "/direct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> uploadDirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        Map<String, String> result = storageService.uploadDirect(
                file.getBytes(),
                folder,
                contentType,
                file.getOriginalFilename());
        return ApiResponse.ok(result);
    }
}
