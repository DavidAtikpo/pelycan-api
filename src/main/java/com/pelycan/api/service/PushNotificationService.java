package com.pelycan.api.service;



import com.google.firebase.FirebaseApp;

import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.messaging.Message;

import com.google.firebase.messaging.Notification;

import com.pelycan.api.config.FirebaseProperties;

import com.pelycan.api.domain.UserFcmToken;

import com.pelycan.api.repository.UserFcmTokenRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;

import org.springframework.stereotype.Service;



import java.util.List;

import java.util.Map;

import java.util.UUID;



@Service

public class PushNotificationService {



    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);



    private final FirebaseApp firebaseApp;

    private final FirebaseProperties properties;

    private final UserFcmTokenRepository tokenRepository;

    private final InAppNotificationService inAppNotificationService;



    public PushNotificationService(

            ObjectProvider<FirebaseApp> firebaseApp,

            FirebaseProperties properties,

            UserFcmTokenRepository tokenRepository,

            InAppNotificationService inAppNotificationService) {

        this.firebaseApp = firebaseApp.getIfAvailable();

        this.properties = properties;

        this.tokenRepository = tokenRepository;

        this.inAppNotificationService = inAppNotificationService;

    }



    public void notifyToken(String token, String title, String body, Map<String, String> data, UUID userId) {

        persistInApp(userId, title, body, data);

        sendFcm(token, title, body, data);

    }



    public void notifyAdmins(String title, String body, Map<String, String> data) {

        if (!properties.adminNotifyEnabled()) {

            return;

        }

        List<UserFcmToken> tokens = tokenRepository.findAllAdminTokens();

        if (tokens.isEmpty()) {

            log.debug("Aucun token FCM admin — notification ignorée");

            return;

        }

        for (UserFcmToken adminToken : tokens) {

            notifyToken(adminToken.getToken(), title, body, data, adminToken.getUserId());

        }

    }



    private void persistInApp(UUID userId, String title, String body, Map<String, String> data) {

        if (userId == null) {

            return;

        }

        String type = data != null && data.get("type") != null ? data.get("type") : "general";

        String senderName = data != null ? data.get("senderName") : null;

        UUID senderId = null;

        if (data != null && data.get("alertUserId") != null) {

            try {

                senderId = UUID.fromString(data.get("alertUserId"));

            } catch (IllegalArgumentException ignored) {

                // ignore invalid UUID

            }

        }

        inAppNotificationService.create(userId, title, body, type, data, senderId, senderName);

    }



    private void sendFcm(String token, String title, String body, Map<String, String> data) {

        if (firebaseApp == null || token == null || token.isBlank()) {

            if (firebaseApp == null && properties.adminNotifyEnabled()) {

                log.debug("Firebase non configuré — push ignoré");

            }

            return;

        }

        try {

            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);

            Message message = Message.builder()

                    .setToken(token)

                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())

                    .putAllData(data != null ? data : Map.of())

                    .build();

            messaging.send(message);

        } catch (Exception e) {

            log.warn("Échec envoi FCM: {}", e.getMessage());

        }

    }

}

