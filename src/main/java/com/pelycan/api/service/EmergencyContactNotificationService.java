package com.pelycan.api.service;

import com.pelycan.api.domain.EmergencyContact;
import com.pelycan.api.domain.User;
import com.pelycan.api.repository.UserFcmTokenRepository;
import com.pelycan.api.repository.UserRepository;
import com.pelycan.api.util.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class EmergencyContactNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyContactNotificationService.class);

    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;
    private final UserFcmTokenRepository tokenRepository;

    public EmergencyContactNotificationService(
            SmsService smsService,
            PushNotificationService pushNotificationService,
            UserRepository userRepository,
            UserFcmTokenRepository tokenRepository) {
        this.smsService = smsService;
        this.pushNotificationService = pushNotificationService;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    public List<Map<String, Object>> notifyContacts(
            User sender,
            List<EmergencyContact> contacts,
            String message,
            String mapsLink) {
        String senderName = (sender.getFirstName() + " " + sender.getLastName()).trim();
        if (senderName.isBlank()) {
            senderName = "Un utilisateur Pelycan";
        }
        String smsBody = buildAlertSms(senderName, message, mapsLink);
        String pushTitle = "Alerte d'urgence — " + senderName;
        String pushBody = message != null && !message.isBlank()
                ? message
                : "Besoin d'aide immédiatement";

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EmergencyContact contact : contacts) {
            rows.add(notifyOneContact(sender, contact, smsBody, pushTitle, pushBody, mapsLink));
        }
        return rows;
    }

    public void notifyContactsResolved(User sender, List<Map<String, Object>> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        String senderName = (sender.getFirstName() + " " + sender.getLastName()).trim();
        if (senderName.isBlank()) {
            senderName = "Votre proche";
        }
        String smsBody = senderName + " vous informe qu'il/elle est maintenant en sécurité. Alerte Pelycan clôturée.";
        String pushTitle = "Alerte clôturée";
        String pushBody = senderName + " est maintenant en sécurité.";

        for (Map<String, Object> recipient : recipients) {
            String phone = string(recipient.get("phone"));
            if (phone == null) {
                continue;
            }
            smsService.send(phone, smsBody);
            notifyPushByPhone(phone, pushTitle, pushBody, Map.of("type", "user_alert_resolved"));
        }
    }

    private Map<String, Object> notifyOneContact(
            User sender,
            EmergencyContact contact,
            String smsBody,
            String pushTitle,
            String pushBody,
            String mapsLink) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("contactId", contact.getId().toString());
        row.put("name", contact.getName());
        row.put("phone", contact.getPhone());

        boolean smsSent = false;
        boolean pushSent = false;
        String error = null;

        SmsService.SmsResult smsResult = smsService.send(contact.getPhone(), smsBody);
        if (smsResult.sent()) {
            smsSent = true;
        } else if (!smsResult.skipped()) {
            error = smsResult.error();
        }

        pushSent = notifyPushByPhone(
                contact.getPhone(),
                pushTitle,
                pushBody,
                Map.of(
                        "type", "user_alert_contact",
                        "alertUserId", sender.getId().toString(),
                        "senderName", (sender.getFirstName() + " " + sender.getLastName()).trim(),
                        "mapsLink", mapsLink != null ? mapsLink : ""));

        boolean notified = smsSent || pushSent;
        row.put("smsSent", smsSent);
        row.put("pushSent", pushSent);
        row.put("notified", notified);
        row.put("error", error);
        if (notified) {
            row.put("notifiedAt", Instant.now().toString());
        }
        return row;
    }

    private boolean notifyPushByPhone(String phone, String title, String body, Map<String, String> data) {
        Optional<User> user = findUserByPhone(phone);
        if (user.isEmpty()) {
            return false;
        }
        return tokenRepository.findByUserId(user.get().getId())
                .map(token -> {
                    pushNotificationService.notifyToken(
                            token.getToken(),
                            title,
                            body,
                            data,
                            user.get().getId());
                    return true;
                })
                .orElse(false);
    }

    private Optional<User> findUserByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        Optional<User> direct = userRepository.findByPhoneNumber(phone.trim());
        if (direct.isPresent()) {
            return direct;
        }
        String normalized = PhoneNormalizer.normalize(phone, "33");
        if (normalized == null) {
            return Optional.empty();
        }
        return userRepository.findByPhoneNumber(normalized);
    }

    private static String buildAlertSms(String senderName, String message, String mapsLink) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALERTE PELYCAN — ").append(senderName);
        sb.append(" a besoin d'aide.");
        if (message != null && !message.isBlank()) {
            sb.append(" ").append(message.trim());
        }
        if (mapsLink != null && !mapsLink.isBlank()) {
            sb.append(" Position: ").append(mapsLink.trim());
        }
        return sb.toString();
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
