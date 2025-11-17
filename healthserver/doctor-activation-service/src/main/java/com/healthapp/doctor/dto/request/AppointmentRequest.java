package com.healthapp.doctor.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotNull(message = "Appointment date and time is required")
    @Future(message = "Appointment must be in the future")
    private LocalDateTime appointmentDateTime;

    @NotBlank(message = "Appointment type is required")
    private String appointmentType; // CONSULTATION, FOLLOW_UP, EMERGENCY

    @NotBlank(message = "Reason is required")
    private String reason;

    private String notes;
}