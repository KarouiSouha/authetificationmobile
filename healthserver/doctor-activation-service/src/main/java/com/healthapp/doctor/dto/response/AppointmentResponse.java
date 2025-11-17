package com.healthapp.doctor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private String id;

    // Patient Info
    private String patientId;
    private String patientEmail;
    private String patientName;
    private String patientPhone;

    // Doctor Info
    private String doctorId;
    private String doctorEmail;
    private String doctorName;
    private String specialization;

    // Appointment Details
    private LocalDateTime appointmentDateTime;
    private String appointmentType;
    private String reason;
    private String notes;
    private String status;

    // Consultation (if completed)
    private String diagnosis;
    private String prescription;
    private String doctorNotes;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
}