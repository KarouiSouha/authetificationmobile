package com.healthapp.doctor.controller;

import com.healthapp.doctor.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/doctors/appointments")
@RequiredArgsConstructor
@Slf4j
public class PublicDoctorAppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Patient cancels their appointment
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<Map<String, String>> cancelAppointmentByPatient(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> body) {

        log.info("❌ Patient cancelling appointment: {}", appointmentId);

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            reason = "No reason provided";
        }

        appointmentService.cancelAppointment(appointmentId, "PATIENT", reason);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Appointment cancelled successfully"
        ));
    }
}
