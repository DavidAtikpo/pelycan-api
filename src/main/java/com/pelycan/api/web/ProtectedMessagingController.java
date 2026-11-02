package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.ProtectedMessagingService;
import com.pelycan.api.web.dto.ApiResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/protected-messaging")
public class ProtectedMessagingController {

    private final ProtectedMessagingService protectedMessagingService;

    public ProtectedMessagingController(ProtectedMessagingService protectedMessagingService) {
        this.protectedMessagingService = protectedMessagingService;
    }

    @GetMapping("/support-inbox")
    public ApiResponse<Map<String, Object>> supportInbox() {
        return ApiResponse.ok(protectedMessagingService.getSupportInbox());
    }

    @GetMapping("/messages/{userId}/{otherUserId}")
    public ApiResponse<List<Map<String, Object>>> messages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID userId,
            @PathVariable UUID otherUserId) {
        if (!user.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return ApiResponse.ok(protectedMessagingService.getConversation(userId, otherUserId));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> message = protectedMessagingService.sendMessage(user.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(message));
    }

    @PutMapping("/{messageId}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable UUID messageId) {
        protectedMessagingService.markAsRead(messageId, user.getId());
        return ApiResponse.okMessage("Message marqué comme lu");
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID messageId) {
        protectedMessagingService.deleteMessage(messageId, user.getId());
        return ApiResponse.okMessage("Message supprimé");
    }
}
