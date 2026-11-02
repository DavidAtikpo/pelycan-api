package com.pelycan.api.service;

import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.LegalConsultationRepository;
import com.pelycan.api.repository.UserRepository;
import com.pelycan.api.web.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "active", "inactive", "suspended", "pending", "banned");

    private final UserRepository userRepository;
    private final LegalConsultationRepository legalConsultationRepository;
    private final AdminCaseService adminCaseService;

    public UserService(
            UserRepository userRepository,
            LegalConsultationRepository legalConsultationRepository,
            AdminCaseService adminCaseService) {
        this.userRepository = userRepository;
        this.legalConsultationRepository = legalConsultationRepository;
        this.adminCaseService = adminCaseService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllForAdmin() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAdminJson)
                .toList();
    }

    @Transactional
    public void updateStatus(UUID userId, String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Statut invalide. Valeurs autorisées: active, inactive, suspended");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(UUID userId, UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle requis");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
        return toProfileJson(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            String phone = request.phone().trim();
            user.setPhoneNumber(phone.isEmpty() ? null : phone);
        }
        if (request.profilePicture() != null) {
            String picture = request.profilePicture().trim();
            user.setProfilePicture(picture.isEmpty() ? null : picture);
        }

        userRepository.save(user);
        return toProfileJson(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProfessionalsForAdmin() {
        return userRepository.findByRoleOrderByCreatedAtDesc(UserRole.pro).stream()
                .map(this::toProfessionalAdminJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminStatistics() {
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(u -> "active".equals(u.getStatus())).count();
        List<User> pros = userRepository.findByRoleOrderByCreatedAtDesc(UserRole.pro);
        long activePros = pros.stream().filter(u -> "active".equals(u.getStatus())).count();
        Map<String, Long> caseStatsRaw = adminCaseService.caseStatistics();
        long totalCases = caseStatsRaw.getOrDefault("total", 0L);
        long inProgressCases = caseStatsRaw.getOrDefault("inProgress", 0L);
        long completedCases = caseStatsRaw.getOrDefault("completed", 0L);
        long urgentCases = caseStatsRaw.getOrDefault("urgent", 0L);
        long totalActiveCases = pros.stream()
                .mapToLong(pro -> adminCaseService.countActiveCasesForProfessional(pro.getId()))
                .sum();
        double averageCaseLoad = pros.isEmpty() ? 0 : (double) totalActiveCases / pros.size();

        Map<String, Object> userStats = Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "newThisMonth", 0);
        Map<String, Object> caseStats = Map.of(
                "total", totalCases,
                "inProgress", inProgressCases,
                "completed", completedCases,
                "urgent", urgentCases);
        Map<String, Object> proStats = Map.of(
                "total", pros.size(),
                "active", activePros,
                "averageCaseLoad", averageCaseLoad);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userStats", userStats);
        stats.put("caseStats", caseStats);
        stats.put("proStats", proStats);
        return stats;
    }

    @Transactional
    public void deleteProfessional(UUID professionalId) {
        User user = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professionnel non trouvé"));
        if (user.getRole() != UserRole.pro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur n'est pas un professionnel");
        }
        userRepository.delete(user);
    }

    private Map<String, Object> toProfessionalAdminJson(User user) {
        long activeCases = adminCaseService.countActiveCasesForProfessional(user.getId());
        long totalCases = legalConsultationRepository
                .findByAssignedToOrderByCreatedAtDesc(user.getId())
                .size();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("firstName", user.getFirstName());
        m.put("lastName", user.getLastName());
        m.put("email", user.getEmail());
        m.put("phoneNumber", user.getPhoneNumber());
        m.put("status", user.getStatus());
        m.put("speciality", null);
        m.put("createdAt", user.getCreatedAt());
        m.put("lastLogin", user.getLastLogin());
        m.put("totalCases", totalCases);
        m.put("activeCases", activeCases);
        return m;
    }

    private Map<String, Object> toProfileJson(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("firstName", user.getFirstName());
        m.put("lastName", user.getLastName());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhoneNumber());
        m.put("phoneNumber", user.getPhoneNumber());
        m.put("role", user.getRole().name());
        m.put("status", user.getStatus());
        m.put("profilePicture", user.getProfilePicture());
        m.put("profile_picture", user.getProfilePicture());
        m.put("createdAt", user.getCreatedAt());
        m.put("updatedAt", user.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toAdminJson(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("firstName", user.getFirstName());
        m.put("lastName", user.getLastName());
        m.put("email", user.getEmail());
        m.put("phoneNumber", user.getPhoneNumber());
        m.put("role", user.getRole().name());
        m.put("status", user.getStatus());
        m.put("lastLogin", user.getLastLogin());
        m.put("createdAt", user.getCreatedAt());
        m.put("updatedAt", user.getUpdatedAt());
        return m;
    }
}
