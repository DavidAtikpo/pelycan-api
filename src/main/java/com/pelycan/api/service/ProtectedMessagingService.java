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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProtectedMessagingService {

    private final ProtectedMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ProtectedMessagingService(
            ProtectedMessageRepository messageRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSupportInbox() {
        User admin = userRepository
                .findByRoleAndStatusOrderByCreatedAtDesc(UserRole.admin, "active")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun administrateur disponible pour le support"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("supportInboxUserId", admin.getId().toString());
        data.put("displayName", admin.getFirstName() + " " + admin.getLastName());
        return data;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConversation(UUID currentUserId, UUID otherUserId) {
        requireParticipant(currentUserId, otherUserId);
        return messageRepository.findConversation(currentUserId, otherUserId).stream()
                .map(message -> toJson(message, currentUserId))
                .toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(UUID senderId, Map<String, Object> body) {
        UUID recipientId = parseUuid(first(body, "recipientId", "recipient_id"), "destinataire");
        String content = string(body.get("content"));
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message est vide");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expéditeur introuvable"));

        boolean senderIsStaff = sender.getRole() == UserRole.pro || sender.getRole() == UserRole.admin;
        boolean recipientIsStaff = recipient.getRole() == UserRole.pro || recipient.getRole() == UserRole.admin;

        if (senderIsStaff && recipient.getRole() != UserRole.user) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinataire invalide");
        }
        if (!senderIsStaff && !recipientIsStaff) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destinataire invalide");
        }

        ProtectedMessage message = new ProtectedMessage();
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setContent(content.trim());
        message.setAttachments(parseAttachments(body.get("attachments")));
        messageRepository.save(message);

        return toJson(message, senderId);
    }

    @Transactional
    public void markAsRead(UUID messageId, UUID readerId) {
        ProtectedMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if (!message.getRecipientId().equals(readerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(Instant.now());
            messageRepository.save(message);
        }
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID requesterId) {
        ProtectedMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if (!message.getSenderId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'expéditeur peut supprimer ce message");
        }

        message.setDeletedBySender(true);
        messageRepository.save(message);
    }

    private void requireParticipant(UUID currentUserId, UUID otherUserId) {
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (!currentUserId.equals(otherUserId)) {
            User other = userRepository.findById(otherUserId).orElseThrow();
            User current = userRepository.findById(currentUserId).orElseThrow();
            boolean currentIsStaff = current.getRole() == UserRole.pro || current.getRole() == UserRole.admin;
            boolean otherIsStaff = other.getRole() == UserRole.pro || other.getRole() == UserRole.admin;
            if (currentIsStaff && other.getRole() != UserRole.user) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation invalide");
            }
            if (!currentIsStaff && !otherIsStaff) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation invalide");
            }
        }
    }

    private Map<String, Object> toJson(ProtectedMessage message, UUID viewerId) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", message.getId().toString());
        json.put("senderId", message.getSenderId().toString());
        json.put("recipientId", message.getRecipientId().toString());
        json.put("content", message.getContent());
        json.put("encrypted", message.isEncrypted());
        json.put("filtered", message.isFiltered());
        json.put("filterReason", message.getFilterReason());
        json.put("inappropriate", message.isInappropriate());
        json.put("read", message.isRead());
        json.put("readAt", message.getReadAt() != null ? message.getReadAt().toString() : null);
        json.put("attachments", message.getAttachments() != null ? message.getAttachments() : List.of());
        json.put("createdAt", message.getCreatedAt().toString());
        json.put("updatedAt", message.getUpdatedAt().toString());

        userRepository.findById(message.getSenderId()).ifPresent(sender -> {
            Map<String, Object> senderJson = new LinkedHashMap<>();
            senderJson.put("name", sender.getFirstName() + " " + sender.getLastName());
            json.put("sender", senderJson);
        });

        return json;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAttachments(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        if (raw instanceof List<?> list) {
            List<String> attachments = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    attachments.add(item.toString());
                }
            }
            return attachments;
        }
        return new ArrayList<>();
    }

    private static UUID parseUuid(Object raw, String label) {
        return parseUuid(raw, label, true);
    }

    private static UUID parseUuid(Object raw, String label, boolean required) {
        String value = string(raw);
        if (value == null || value.isBlank()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " manquant");
            }
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " invalide");
        }
    }

    private static Object first(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return body.get(key);
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }
}
