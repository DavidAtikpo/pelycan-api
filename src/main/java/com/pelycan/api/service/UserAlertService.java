package com.pelycan.api.service;

import com.pelycan.api.domain.*;
import com.pelycan.api.repository.AlertLocationRepository;
import com.pelycan.api.repository.EmergencyContactRepository;
import com.pelycan.api.repository.UserAlertRepository;
import com.pelycan.api.repository.UserMedicalInfoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.time.Instant;

@Service
public class UserAlertService {

    private final UserAlertRepository userAlertRepository;
    private final AlertLocationRepository alertLocationRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final AgoraTokenService agoraTokenService;
    private final PushNotificationService pushNotificationService;
    private final UserMedicalInfoRepository userMedicalInfoRepository;
    private final EmergencyContactNotificationService contactNotificationService;
    private final SmsService smsService;

    public UserAlertService(
            UserAlertRepository userAlertRepository,
            AlertLocationRepository alertLocationRepository,
            EmergencyContactRepository emergencyContactRepository,
            AgoraTokenService agoraTokenService,
            PushNotificationService pushNotificationService,
            UserMedicalInfoRepository userMedicalInfoRepository,
            EmergencyContactNotificationService contactNotificationService,
            SmsService smsService) {
        this.userAlertRepository = userAlertRepository;
        this.alertLocationRepository = alertLocationRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.agoraTokenService = agoraTokenService;
        this.pushNotificationService = pushNotificationService;
        this.userMedicalInfoRepository = userMedicalInfoRepository;
        this.contactNotificationService = contactNotificationService;
        this.smsService = smsService;
    }

    @Transactional
    public Map<String, Object> triggerAdmin(User user, String message, Double latitude, Double longitude) {
        ensureNoActiveAlert(user.getId());
        UserAlert alert = buildAlert(user.getId(), message, latitude, longitude,
                "⚠️ ALERTE ENVOYÉE À L'ADMINISTRATION");
        userAlertRepository.save(alert);
        saveLocation(alert.getId(), latitude, longitude);
        pushNotificationService.notifyAdmins(
                "Alerte détresse",
                user.getFirstName() + " " + user.getLastName() + " — alerte administration",
                Map.of("alertId", alert.getId().toString(), "type", "user_alert_admin"));
        String maps = mapsLink(latitude, longitude);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("alert", toUserDto(alert));
        response.put("googleMapsLink", maps != null ? maps : "");
        return response;
    }

    @Transactional
    public Map<String, Object> trigger(User user, String message, Double latitude, Double longitude, List<UUID> contactIds) {
        ensureNoActiveAlert(user.getId());
        List<EmergencyContact> contacts = resolveContacts(user.getId(), contactIds);
        if (contacts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun contact d'urgence configuré. Ajoutez des contacts avant de déclencher une alerte.");
        }
        UserAlert alert = buildAlert(user.getId(), message, latitude, longitude, "⚠️ ALERTE D'URGENCE");
        String maps = mapsLink(latitude, longitude);
        List<Map<String, Object>> recipientRows = contactNotificationService.notifyContacts(
                user, contacts, alert.getMessage(), maps);
        long contactsNotified = recipientRows.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("notified")))
                .count();

        alert.setContactIds(contacts.stream().map(c -> c.getId().toString()).toList());
        alert.setRecipients(recipientRows);
        userAlertRepository.save(alert);
        saveLocation(alert.getId(), latitude, longitude);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("alert", alert);
        response.put("contactsTotal", contacts.size());
        response.put("contactsNotified", contactsNotified);
        response.put("smsEnabled", smsService.isConfigured());
        response.put("smsFallbackRecommended", !smsService.isConfigured() || contactsNotified < contacts.size());
        response.put("googleMapsLink", maps);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listeningToken(UUID alertId, User user) {
        UserAlert alert = userAlertRepository.findById(alertId)
                .filter(a -> a.getStatus() == AlertStatus.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte active non trouvée"));
        boolean owner = alert.getUserId().equals(user.getId());
        boolean admin = user.getRole() == UserRole.admin || user.getRole() == UserRole.pro;
        if (!owner && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce canal d'écoute");
        }
        String channelId = "distress-" + alertId;
        int uid = 0;
        String token = agoraTokenService.buildPublisherToken(channelId, uid);
        return Map.of("token", token, "channelId", channelId, "uid", uid);
    }

    @Transactional
    public void resolveOwn(UUID alertId, User user) {
        UserAlert alert = findActiveForUser(alertId, user.getId());
        List<Map<String, Object>> recipients = alert.getRecipients();
        alert.resolve();
        userAlertRepository.save(alert);
        if (recipients != null && !recipients.isEmpty()) {
            contactNotificationService.notifyContactsResolved(user, recipients);
        }
    }

    @Transactional
    public void cancelOwn(UUID alertId, UUID userId) {
        UserAlert alert = findActiveForUser(alertId, userId);
        alert.cancel();
        userAlertRepository.save(alert);
    }

    @Transactional
    public void resolveAdmin(UUID alertId) {
        UserAlert alert = userAlertRepository.findById(alertId)
                .filter(a -> a.getStatus() == AlertStatus.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte active non trouvée"));
        alert.resolve();
        userAlertRepository.save(alert);
    }

    @Transactional
    public void cancelAdmin(UUID alertId) {
        UserAlert alert = userAlertRepository.findById(alertId)
                .filter(a -> a.getStatus() == AlertStatus.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte active non trouvée"));
        alert.cancel();
        userAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listActiveForAdmin() {
        return userAlertRepository.findByStatusWithUser(AlertStatus.active).stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllForAdmin(int limit) {
        return userAlertRepository.findAllWithUser().stream()
                .limit(limit)
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional
    public Map<String, Object> alertContacts(User user, List<UUID> contactIds, String message, Map<String, Object> location) {
        if (contactIds == null || contactIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un contact est requis");
        }
        ensureNoActiveAlert(user.getId());
        Double lat = location != null ? dbl(location.get("latitude")) : null;
        Double lng = location != null ? dbl(location.get("longitude")) : null;
        String address = location != null ? string(location.get("address")) : null;
        List<EmergencyContact> contacts = emergencyContactRepository.findAllById(contactIds).stream()
                .filter(c -> c.getUserId().equals(user.getId()))
                .toList();
        if (contacts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contacts invalides");
        }
        UserAlert alert = new UserAlert();
        alert.setUserId(user.getId());
        alert.setMessage(message != null && !message.isBlank()
                ? message
                : "Je suis en danger. J'ai besoin d'aide immédiatement.");
        alert.setLatitude(lat);
        alert.setLongitude(lng);
        alert.setLocation(address != null ? address : mapsLink(lat, lng));
        alert.setStatus(AlertStatus.active);
        alert.setContactIds(contactIds.stream().map(UUID::toString).toList());
        String maps = mapsLink(lat, lng);
        List<Map<String, Object>> recipients = contactNotificationService.notifyContacts(
                user, contacts, alert.getMessage(), maps);
        alert.setRecipients(recipients);
        userAlertRepository.save(alert);
        if (lat != null && lng != null) {
            saveLocation(alert.getId(), lat, lng);
        }
        return Map.of(
                "alertId", alert.getId(),
                "contacts", recipients,
                "message", alert.getMessage(),
                "location", alert.getLocation() != null ? alert.getLocation() : "",
                "status", alert.getStatus().name(),
                "createdAt", alert.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(UUID userId, String status, int limit) {
        List<UserAlert> alerts;
        if (status != null && !status.isBlank()) {
            AlertStatus st = AlertStatus.valueOf(status);
            alerts = userAlertRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, st);
        } else {
            alerts = userAlertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return alerts.stream().limit(limit).map(this::toUserDto).toList();
    }

    @Transactional(readOnly = true)
    public long countActiveForUser(UUID userId) {
        return userAlertRepository.countByUserIdAndStatus(userId, AlertStatus.active);
    }

    @Transactional
    public Map<String, Object> updateLocation(UUID alertId, UUID userId, Double latitude, Double longitude) {
        UserAlert alert = findActiveForUser(alertId, userId);
        saveLocation(alertId, latitude, longitude);
        alert.setLatitude(latitude);
        alert.setLongitude(longitude);
        alert.setLocation(mapsLink(latitude, longitude));
        userAlertRepository.save(alert);
        return Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "googleMapsLink", alert.getLocation());
    }

    @Transactional
    public Map<String, Object> resolveForSecurity(UUID alertId, UUID userId) {
        UserAlert alert = userAlertRepository.findById(alertId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        alert.resolve();
        userAlertRepository.save(alert);
        return toUserDto(alert);
    }

    @Transactional
    public Map<String, Object> cancelForSecurity(UUID alertId, UUID userId) {
        UserAlert alert = userAlertRepository.findById(alertId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte non trouvée"));
        alert.cancel();
        userAlertRepository.save(alert);
        return toUserDto(alert);
    }

    @Transactional
    public UserMedicalInfo getOrCreateMedicalInfo(UUID userId) {
        return userMedicalInfoRepository.findById(userId).orElseGet(() -> {
            UserMedicalInfo info = new UserMedicalInfo();
            info.setUserId(userId);
            return userMedicalInfoRepository.save(info);
        });
    }

    @Transactional
    public UserMedicalInfo updateMedicalInfo(UUID userId, Map<String, Object> body) {
        UserMedicalInfo info = getOrCreateMedicalInfo(userId);
        if (body.containsKey("blood_type")) {
            info.setBloodType(string(body.get("blood_type")));
        }
        if (body.containsKey("allergies")) {
            info.setAllergies(string(body.get("allergies")));
        }
        if (body.containsKey("medical_conditions")) {
            info.setMedicalConditions(string(body.get("medical_conditions")));
        }
        if (body.containsKey("emergency_notes")) {
            info.setEmergencyNotes(string(body.get("emergency_notes")));
        }
        return userMedicalInfoRepository.save(info);
    }

    private UserAlert findActiveForUser(UUID alertId, UUID userId) {
        return userAlertRepository.findById(alertId)
                .filter(a -> a.getUserId().equals(userId) && a.getStatus() == AlertStatus.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerte active non trouvée"));
    }

    private void ensureNoActiveAlert(UUID userId) {
        userAlertRepository.findFirstByUserIdAndStatus(userId, AlertStatus.active).ifPresent(a -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous avez déjà une alerte active");
        });
    }

    private UserAlert buildAlert(UUID userId, String message, Double lat, Double lng, String defaultMsg) {
        UserAlert alert = new UserAlert();
        alert.setUserId(userId);
        alert.setMessage(message != null && !message.isBlank() ? message : defaultMsg);
        alert.setLatitude(lat);
        alert.setLongitude(lng);
        alert.setLocation(mapsLink(lat, lng));
        alert.setStatus(AlertStatus.active);
        return alert;
    }

    private void saveLocation(UUID alertId, Double lat, Double lng) {
        if (lat == null || lng == null) {
            return;
        }
        AlertLocation loc = new AlertLocation();
        loc.setAlertId(alertId);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        alertLocationRepository.save(loc);
    }

    private List<EmergencyContact> resolveContacts(UUID userId, List<UUID> contactIds) {
        if (contactIds != null && !contactIds.isEmpty()) {
            return emergencyContactRepository.findAllById(contactIds).stream()
                    .filter(c -> c.getUserId().equals(userId))
                    .toList();
        }
        return emergencyContactRepository.findByUserId(userId).stream().limit(5).toList();
    }

    private static String mapsLink(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        return "https://www.google.com/maps?q=" + lat + "," + lng;
    }

    private Map<String, Object> toAdminDto(UserAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("userId", alert.getUserId());
        m.put("message", alert.getMessage());
        m.put("location", alert.getLocation());
        m.put("latitude", alert.getLatitude());
        m.put("longitude", alert.getLongitude());
        m.put("status", alert.getStatus().name());
        m.put("createdAt", alert.getCreatedAt());
        if (alert.getUser() != null) {
            User u = alert.getUser();
            m.put("user", Map.of(
                    "id", u.getId(),
                    "firstName", u.getFirstName(),
                    "lastName", u.getLastName(),
                    "fullName", (u.getFirstName() + " " + u.getLastName()).trim(),
                    "email", u.getEmail(),
                    "phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : ""));
        }
        return m;
    }

    private Map<String, Object> toUserDto(UserAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", alert.getId());
        m.put("userId", alert.getUserId());
        m.put("message", alert.getMessage());
        m.put("location", alert.getLocation());
        m.put("latitude", alert.getLatitude());
        m.put("longitude", alert.getLongitude());
        m.put("status", alert.getStatus().name());
        m.put("contactIds", alert.getContactIds());
        m.put("recipients", alert.getRecipients());
        m.put("createdAt", alert.getCreatedAt());
        m.put("resolvedAt", alert.getResolvedAt());
        m.put("cancelledAt", alert.getCancelledAt());
        return m;
    }

    private static Double dbl(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(o));
    }

    private static String string(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
