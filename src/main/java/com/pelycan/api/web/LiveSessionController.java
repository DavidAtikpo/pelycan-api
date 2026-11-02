package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.LiveSessionService;
import com.pelycan.api.web.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/groupes-parole/live")
public class LiveSessionController {

    private final LiveSessionService liveSessionService;

    public LiveSessionController(LiveSessionService liveSessionService) {
        this.liveSessionService = liveSessionService;
    }

    @GetMapping("/active")
    public List<Map<String, Object>> active() {
        return liveSessionService.listActive();
    }

    @GetMapping("/all")
    public List<Map<String, Object>> all() {
        return liveSessionService.listAll();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('admin','pro')")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(liveSessionService.create(user, body));
    }

    @GetMapping("/{sessionId}/participants")
    public Map<String, Object> participants(@PathVariable UUID sessionId) {
        return liveSessionService.listParticipants(sessionId);
    }

    @GetMapping("/{sessionId}/participant/{agoraUid}")
    public Map<String, Object> participantByUid(
            @PathVariable UUID sessionId,
            @PathVariable long agoraUid) {
        return liveSessionService.getParticipantByAgoraUid(sessionId, agoraUid);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return liveSessionService.getById(id);
    }

    @PostMapping("/{id}/join")
    public Map<String, Object> join(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return liveSessionService.join(id, user);
    }

    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leave(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        liveSessionService.leave(id, user.getId());
        return ApiResponse.okMessage("Vous avez quitté la session");
    }

    @PatchMapping("/{id}/audio")
    public ApiResponse<Void> audio(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        liveSessionService.updateAudio(id, user.getId(), bool(body.get("is_muted")));
        return ApiResponse.okMessage("Audio mis à jour");
    }

    @PatchMapping("/{id}/video")
    public ApiResponse<Void> video(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        liveSessionService.updateVideo(id, user.getId(), bool(body.get("is_enabled")));
        return ApiResponse.okMessage("Vidéo mise à jour");
    }

    @PatchMapping("/{id}/hand")
    public ApiResponse<Void> hand(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        liveSessionService.updateHand(id, user.getId(), bool(body.get("is_raised")));
        return ApiResponse.okMessage("Main mise à jour");
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('admin','pro')")
    public ApiResponse<Void> end(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        liveSessionService.end(id, user);
        return ApiResponse.okMessage("Session terminée");
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        return o != null && Boolean.parseBoolean(String.valueOf(o));
    }
}
