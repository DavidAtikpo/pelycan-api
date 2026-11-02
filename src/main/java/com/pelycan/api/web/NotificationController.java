package com.pelycan.api.web;



import com.pelycan.api.domain.User;

import com.pelycan.api.domain.UserRole;

import com.pelycan.api.service.FcmTokenService;

import com.pelycan.api.service.InAppNotificationService;

import com.pelycan.api.web.dto.ApiResponse;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;



import java.util.List;

import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/notifications")

public class NotificationController {



    private final FcmTokenService fcmTokenService;

    private final InAppNotificationService inAppNotificationService;



    public NotificationController(

            FcmTokenService fcmTokenService,

            InAppNotificationService inAppNotificationService) {

        this.fcmTokenService = fcmTokenService;

        this.inAppNotificationService = inAppNotificationService;

    }



    @PostMapping("/register-token")

    public ApiResponse<Void> registerToken(

            @AuthenticationPrincipal User user,

            @RequestBody Map<String, Object> body) {

        String fcmToken = string(body.get("fcmToken"));

        if (fcmToken == null || fcmToken.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token FCM requis");

        }

        String platform = string(body.get("platform"));

        fcmTokenService.register(user.getId(), fcmToken.trim(), platform);

        return ApiResponse.okMessage("Token FCM enregistré avec succès");

    }



    @GetMapping("/user")
    public ApiResponse<List<Map<String, Object>>> listForUser(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String filter) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return ApiResponse.ok(inAppNotificationService.listForUser(user.getId(), filter));
    }



    @GetMapping("/admin")

    public ApiResponse<List<Map<String, Object>>> listForAdmin(

            @AuthenticationPrincipal User user,

            @RequestParam(required = false) String filter) {

        if (user.getRole() != UserRole.admin && user.getRole() != UserRole.pro) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");

        }

        return ApiResponse.ok(inAppNotificationService.listForUser(user.getId(), filter));

    }



    @GetMapping("/unread-count")

    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {

        return ApiResponse.ok(Map.of("count", inAppNotificationService.countUnread(user.getId())));

    }



    @PutMapping("/{notificationId}/read")

    public ApiResponse<Void> markRead(

            @AuthenticationPrincipal User user,

            @PathVariable UUID notificationId) {

        inAppNotificationService.markRead(user.getId(), notificationId);

        return ApiResponse.okMessage("Notification marquée comme lue");

    }



    @PutMapping("/read-all")

    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal User user) {

        inAppNotificationService.markAllRead(user.getId());

        return ApiResponse.okMessage("Toutes les notifications ont été marquées comme lues");

    }



    @DeleteMapping("/{notificationId}")

    public ApiResponse<Void> delete(

            @AuthenticationPrincipal User user,

            @PathVariable UUID notificationId) {

        inAppNotificationService.delete(user.getId(), notificationId);

        return ApiResponse.okMessage("Notification supprimée");

    }



    private static String string(Object o) {

        return o == null ? null : String.valueOf(o);

    }

}

