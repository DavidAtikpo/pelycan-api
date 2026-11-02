package com.pelycan.api.service;

import com.pelycan.api.domain.ProtectedMessage;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.ProtectedMessageRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MessagesBridgeService {

    private final ProtectedMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProtectedMessagingService protectedMessagingService;

    public MessagesBridgeService(
            ProtectedMessageRepository messageRepository,
            UserRepository userRepository,
            ProtectedMessagingService protectedMessagingService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.protectedMessagingService = protectedMessagingService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listConversations(UUID currentUserId) {
        List<ProtectedMessage> messages = messageRepository.findAllForUser(currentUserId);
        Map<UUID, ConversationAccumulator> conversations = new HashMap<>();

        for (ProtectedMessage message : messages) {
            UUID partnerId = message.getSenderId().equals(currentUserId)
                    ? message.getRecipientId()
                    : message.getSenderId();
            conversations.computeIfAbsent(partnerId, ConversationAccumulator::new)
                    .add(message, currentUserId);
        }

        return conversations.values().stream()
                .map(ConversationAccumulator::toJson)
                .sorted(Comparator.comparing(
                        (Map<String, Object> c) -> string(c.get("updatedAt")),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMessages(UUID currentUserId, String conversationId) {
        UUID otherUserId = parseUuid(conversationId, "conversation");
        return protectedMessagingService.getConversation(currentUserId, otherUserId).stream()
                .map(message -> {
                    Map<String, Object> json = new LinkedHashMap<>(message);
                    json.put("content", message.get("content"));
                    json.put("createdAt", message.get("createdAt"));
                    json.put("senderId", message.get("senderId"));
                    json.put("isMine", currentUserId.toString().equals(message.get("senderId")));
                    return json;
                })
                .toList();
    }

    @Transactional
    public void sendMessage(UUID currentUserId, String conversationId, String content) {
        UUID recipientId = parseUuid(conversationId, "conversation");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("recipientId", recipientId.toString());
        body.put("content", content);
        protectedMessagingService.sendMessage(currentUserId, body);
    }

    @Transactional
    public void markConversationRead(UUID currentUserId, String conversationId) {
        UUID otherUserId = parseUuid(conversationId, "conversation");
        List<ProtectedMessage> messages = messageRepository.findConversation(currentUserId, otherUserId);
        for (ProtectedMessage message : messages) {
            if (message.getRecipientId().equals(currentUserId) && !message.isRead()) {
                protectedMessagingService.markAsRead(message.getId(), currentUserId);
            }
        }
    }

    private static UUID parseUuid(String raw, String label) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " invalide");
        }
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private class ConversationAccumulator {
        private final UUID partnerId;
        private ProtectedMessage lastMessage;
        private int unreadCount;

        ConversationAccumulator(UUID partnerId) {
            this.partnerId = partnerId;
        }

        void add(ProtectedMessage message, UUID currentUserId) {
            if (lastMessage == null || message.getCreatedAt().isAfter(lastMessage.getCreatedAt())) {
                lastMessage = message;
            }
            if (message.getRecipientId().equals(currentUserId) && !message.isRead()) {
                unreadCount++;
            }
        }

        Map<String, Object> toJson() {
            User partner = userRepository.findById(partnerId).orElse(null);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", partnerId.toString());
            json.put("userId", partnerId.toString());
            json.put("userName", partner != null
                    ? partner.getFirstName() + " " + partner.getLastName()
                    : "Utilisateur");
            json.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
            json.put("unreadCount", unreadCount);
            json.put("updatedAt", lastMessage != null ? lastMessage.getCreatedAt().toString() : null);
            if (partner != null && partner.getRole() == UserRole.user) {
                json.put("participantType", "user");
            }
            return json;
        }
    }
}
