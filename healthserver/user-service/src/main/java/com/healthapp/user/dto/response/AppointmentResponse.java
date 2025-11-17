package com.healthapp.user.dto.response;

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
    private String patientId;
    private String patientEmail;
    private String patientName;
    private String patientPhone;
    private String doctorId;
    private String doctorEmail;
    private String doctorName;
    private String specialization;
    private LocalDateTime appointmentDateTime;
    private String appointmentType;
    private String reason;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
}