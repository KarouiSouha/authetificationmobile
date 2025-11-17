package com.healthapp.doctor.controller;

import com.healthapp.doctor.dto.response.AppointmentResponse;
import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import com.healthapp.doctor.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Public Doctor Controller - No authentication required
 * Used for patient interactions and inter-service communication
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
public class PublicDoctorController {

    private final DoctorRepository doctorRepository;
    private final AppointmentService appointmentService;

    /**
     * Get all activated doctors (for patient to choose)
     */
    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> getActivatedDoctors() {
        log.info("🩺 Fetching available doctors");

        List<Doctor> doctors = doctorRepository.findByIsActivatedTrue();

        List<Map<String, Object>> response = doctors.stream()
                .map(doctor -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doctor.getId());
                    map.put("fullName", doctor.getFullName());
                    map.put("email", doctor.getEmail());
                    map.put("specialization", doctor.getSpecialization());
                    map.put("hospitalAffiliation", doctor.getHospitalAffiliation());
                    map.put("yearsOfExperience", doctor.getYearsOfExperience());
                    map.put("officeAddress", doctor.getOfficeAddress() != null ? doctor.getOfficeAddress() : "");
                    map.put("consultationHours", doctor.getConsultationHours() != null ? doctor.getConsultationHours() : "");
                    map.put("profilePictureUrl", doctor.getProfilePictureUrl() != null ? doctor.getProfilePictureUrl() : "");
                    return map;
                })
                .collect(Collectors.toList());

        log.info("✅ Found {} activated doctors", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Create appointment from patient (via user-service)
     */
    @PostMapping("/appointments/from-patient")
    public ResponseEntity<Map<String, Object>> createAppointmentFromPatient(
            @RequestBody Map<String, Object> request) {

        log.info("📅 Creating appointment from patient");

        String doctorId = (String) request.get("doctorId");
        String patientId = (String) request.get("patientId");
        String patientEmail = (String) request.get("patientEmail");
        String patientName = (String) request.get("patientName");
        String patientPhone = (String) request.get("patientPhone");
        String appointmentDateTime = (String) request.get("appointmentDateTime");
        String appointmentType = (String) request.get("appointmentType");
        String reason = (String) request.get("reason");
        String notes = (String) request.get("notes");

        // Create appointment request
        com.healthapp.doctor.dto.request.AppointmentRequest appointmentRequest =
                com.healthapp.doctor.dto.request.AppointmentRequest.builder()
                        .doctorId(doctorId)
                        .appointmentDateTime(LocalDateTime.parse(appointmentDateTime))
                        .appointmentType(appointmentType)
                        .reason(reason)
                        .notes(notes)
                        .build();

        AppointmentResponse response = appointmentService.createAppointment(
                appointmentRequest, patientId, patientEmail, patientName);

        // Convert to Map for Feign
        Map<String, Object> map = new HashMap<>();
        map.put("id", response.getId());
        map.put("patientId", response.getPatientId());
        map.put("patientEmail", response.getPatientEmail());
        map.put("patientName", response.getPatientName());
        map.put("patientPhone", response.getPatientPhone() != null ? response.getPatientPhone() : "");
        map.put("doctorId", response.getDoctorId());
        map.put("doctorEmail", response.getDoctorEmail());
        map.put("doctorName", response.getDoctorName());
        map.put("specialization", response.getSpecialization());
        map.put("appointmentDateTime", response.getAppointmentDateTime().toString());
        map.put("appointmentType", response.getAppointmentType());
        map.put("reason", response.getReason());
        map.put("notes", response.getNotes() != null ? response.getNotes() : "");
        map.put("status", response.getStatus());
        map.put("createdAt", response.getCreatedAt().toString());

        return ResponseEntity.ok(map);
    }

    /**
     * Get patient appointments (via user-service)
     */
    @GetMapping("/appointments/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getPatientAppointments(
            @PathVariable String patientId) {

        log.info("📅 Fetching appointments for patient: {}", patientId);

        List<AppointmentResponse> appointments = appointmentService.getPatientAppointments(patientId);

        List<Map<String, Object>> response = appointments.stream()
                .map(appt -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", appt.getId());
                    map.put("patientId", appt.getPatientId());
                    map.put("patientEmail", appt.getPatientEmail());
                    map.put("patientName", appt.getPatientName());
                    map.put("patientPhone", appt.getPatientPhone() != null ? appt.getPatientPhone() : "");
                    map.put("doctorId", appt.getDoctorId());
                    map.put("doctorEmail", appt.getDoctorEmail());
                    map.put("doctorName", appt.getDoctorName());
                    map.put("specialization", appt.getSpecialization());
                    map.put("appointmentDateTime", appt.getAppointmentDateTime().toString());
                    map.put("appointmentType", appt.getAppointmentType());
                    map.put("reason", appt.getReason());
                    map.put("notes", appt.getNotes() != null ? appt.getNotes() : "");
                    map.put("status", appt.getStatus());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
