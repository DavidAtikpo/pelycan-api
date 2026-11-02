package com.pelycan.api.web;

import com.pelycan.api.domain.User;
import com.pelycan.api.service.MessagesBridgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

    private final MessagesBridgeService messagesBridgeService;

    public MessagesController(MessagesBridgeService messagesBridgeService) {
        this.messagesBridgeService = messagesBridgeService;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> conversations(@AuthenticationPrincipal User user) {
        return messagesBridgeService.listConversations(user.getId());
    }

    @GetMapping("/{conversationId}")
    public List<Map<String, Object>> messages(
            @AuthenticationPrincipal User user,
            @PathVariable String conversationId) {
        return messagesBridgeService.getMessages(user.getId(), conversationId);
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        String conversationId = body.get("conversationId") != null
                ? body.get("conversationId").toString()
                : body.get("conversation_id").toString();
        String content = body.get("content") != null ? body.get("content").toString() : "";
        messagesBridgeService.sendMessage(user.getId(), conversationId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true));
    }

    @PostMapping("/{conversationId}/read")
    public Map<String, Object> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable String conversationId) {
        messagesBridgeService.markConversationRead(user.getId(), conversationId);
        return Map.of("success", true);
    }
}
