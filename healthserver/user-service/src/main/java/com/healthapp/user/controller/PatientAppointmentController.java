package com.healthapp.user.controller;

import com.healthapp.user.dto.request.AppointmentRequest;
import com.healthapp.user.dto.response.ApiResponse;
import com.healthapp.user.dto.response.AppointmentResponse;
import com.healthapp.user.security.CustomUserPrincipal;
import com.healthapp.user.service.AppointmentClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Patient Appointment Controller
 * Handles appointment operations for patients
 */
@RestController
@RequestMapping("/api/v1/appointments")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Slf4j
public class PatientAppointmentController {

    private final AppointmentClientService appointmentService;

    /**
     * Create a new appointment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentRequest request,
            Authentication auth) {

        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();

        log.info("📅 Patient {} creating appointment with doctor {}",
                principal.getEmail(), request.getDoctorId());

        AppointmentResponse response = appointmentService.createAppointment(request, principal);

        return ResponseEntity.ok(ApiResponse.success("Appointment created successfully", response));
    }

    /**
     * Get all appointments for the authenticated patient
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(Authentication auth) {
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();

        log.info("📅 Patient {} requesting appointments", principal.getEmail());

        List<AppointmentResponse> appointments = appointmentService.getPatientAppointments(principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved", appointments));
    }

    /**
     * Cancel an appointment
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();

        log.info("❌ Patient {} cancelling appointment: {}", principal.getEmail(), appointmentId);

        String reason = body.get("reason");

        appointmentService.cancelAppointment(appointmentId,  reason);

        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", null));
    }

    /**
     * Get available doctors (all activated doctors)
     */
    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableDoctors() {
        log.info("🩺 Fetching available doctors");

        List<Map<String, Object>> doctors = appointmentService.getAvailableDoctors();

        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved", doctors));
    }
}