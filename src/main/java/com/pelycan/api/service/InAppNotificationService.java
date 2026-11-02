package com.pelycan.api.service;

import com.pelycan.api.domain.UserNotification;
import com.pelycan.api.repository.UserNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class InAppNotificationService {

    private final UserNotificationRepository repository;

    public InAppNotificationService(UserNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserNotification create(
            UUID userId,
            String title,
            String body,
            String type,
            Map<String, ?> data,
            UUID senderId,
            String senderName) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTitle(title != null && !title.isBlank() ? title.trim() : "Notification");
        notification.setBody(body != null ? body.trim() : "");
        notification.setType(type != null && !type.isBlank() ? type.trim() : "general");
        notification.setData(toObjectMap(data));
        notification.setSenderId(senderId);
        notification.setSenderName(senderName);
        notification.setRead(false);
        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(UUID userId, String filter) {
        List<UserNotification> rows;
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            rows = repository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if ("unread".equalsIgnoreCase(filter)) {
            rows = repository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
        } else {
            rows = repository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, filter);
        }
        return rows.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        UserNotification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        if (!notification.isRead()) {
            notification.markRead();
            repository.save(notification);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        repository.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void delete(UUID userId, UUID notificationId) {
        UserNotification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        repository.delete(notification);
    }

    private Map<String, Object> toDto(UserNotification notification) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", notification.getId().toString());
        dto.put("title", notification.getTitle());
        dto.put("body", notification.getBody());
        dto.put("type", notification.getType());
        dto.put("data", notification.getData() != null ? notification.getData() : Map.of());
        dto.put("created_at", notification.getCreatedAt());
        dto.put("createdAt", notification.getCreatedAt());
        dto.put("is_read", notification.isRead());
        dto.put("isRead", notification.isRead());
        if (notification.getSenderId() != null) {
            dto.put("sender_id", notification.getSenderId().toString());
            dto.put("senderId", notification.getSenderId().toString());
        }
        if (notification.getSenderName() != null) {
            dto.put("sender_name", notification.getSenderName());
            dto.put("senderName", notification.getSenderName());
        }
        return dto;
    }

    private static Map<String, Object> toObjectMap(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        data.forEach(copy::put);
        return copy;
    }
}
