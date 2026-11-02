package com.pelycan.api.service;

import com.pelycan.api.domain.ProfessionalAppointment;
import com.pelycan.api.domain.User;
import com.pelycan.api.domain.UserRole;
import com.pelycan.api.repository.ProfessionalAppointmentRepository;
import com.pelycan.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AppointmentManagementService {

    private final ProfessionalAppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentManagementService(
            ProfessionalAppointmentRepository appointmentRepository,
            UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listForProfessional(UUID professionalId, String status) {
        requireProfessional(professionalId);
        List<Map<String, Object>> appointments =
                appointmentRepository.findByProfessionalIdOrderByAppointmentDateDesc(professionalId).stream()
                        .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase(a.getStatus()))
                        .map(this::toJson)
                        .toList();
        return Map.of("appointments", appointments);
    }

    @Transactional
    public void accept(UUID professionalId, UUID appointmentId) {
        updateStatus(professionalId, appointmentId, "confirmed");
    }

    @Transactional
    public void reject(UUID professionalId, UUID appointmentId, String reason) {
        ProfessionalAppointment appointment = findOwned(professionalId, appointmentId);
        appointment.setStatus("rejected");
        appointment.setRejectionReason(reason);
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void reschedule(UUID professionalId, UUID appointmentId, Instant newDate) {
        ProfessionalAppointment appointment = findOwned(professionalId, appointmentId);
        if (newDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nouvelle date requise");
        }
        appointment.setAppointmentDate(newDate);
        appointment.setStatus("pending");
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void cancel(UUID professionalId, UUID appointmentId) {
        updateStatus(professionalId, appointmentId, "cancelled");
    }

    private void updateStatus(UUID professionalId, UUID appointmentId, String status) {
        ProfessionalAppointment appointment = findOwned(professionalId, appointmentId);
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }

    private ProfessionalAppointment findOwned(UUID professionalId, UUID appointmentId) {
        requireProfessional(professionalId);
        return appointmentRepository.findByIdAndProfessionalId(appointmentId, professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
    }

    private Map<String, Object> toJson(ProfessionalAppointment appointment) {
        User client = userRepository.findById(appointment.getUserId()).orElse(null);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", appointment.getId().toString());
        json.put("appointment_date", appointment.getAppointmentDate().toString());
        json.put("status", appointment.getStatus());
        json.put("title", appointment.getTitle());
        json.put("notes", appointment.getNotes());
        json.put("location", appointment.getLocation());
        json.put("duration_minutes", appointment.getDurationMinutes());
        json.put("rejection_reason", appointment.getRejectionReason());
        if (client != null) {
            json.put("client_name", client.getFirstName() + " " + client.getLastName());
            json.put("client_email", client.getEmail());
        }
        return json;
    }

    private void requireProfessional(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        if (user.getRole() != UserRole.pro && user.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux professionnels");
        }
    }
}
