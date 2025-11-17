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
public class PatientInfoResponse {

    private String patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;

    private Integer totalAppointments;
    private Integer completedAppointments;
    private Integer cancelledAppointments;

    private LocalDateTime lastAppointmentDate;
    private LocalDateTime nextAppointmentDate;
    private LocalDateTime firstVisitDate;
}