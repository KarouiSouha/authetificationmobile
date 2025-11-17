package com.healthapp.videocall.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationService - Send notifications for urgent appointments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:8084";
    
    /**
     * Notify doctor of urgent appointment created from alert
     */
    public void notifyDoctorOfUrgentAppointment(String appointmentId) {
        log.info("📧 Notifying doctor of urgent appointment: {}", appointmentId);
        
        try {
            // Prepare email notification request
            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("to", "doctor@example.com"); // TODO: Get doctor email
            emailRequest.put("subject", "🚨 URGENT Appointment - Camera Alert");
            emailRequest.put("templateType", "URGENT_APPOINTMENT_NOTIFICATION");
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("appointmentId", appointmentId);
            variables.put("alertType", "CAMERA_ALERT");
            emailRequest.put("templateVariables", variables);
            
            // Call notification service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailRequest, headers);
            
            String url = NOTIFICATION_SERVICE_URL + "/api/notifications/email";
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ Doctor notification sent successfully");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send doctor notification: {}", e.getMessage());
            // Don't throw - notification failure shouldn't break alert processing
        }
    }
    
    /**
     * Notify patient that appointment was created
     */
    public void notifyPatientOfAppointment(String patientEmail, String appointmentId) {
        log.info("📧 Notifying patient: {}", patientEmail);
        
        try {
            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("to", patientEmail);
            emailRequest.put("subject", "Appointment Created - Camera Alert");
            emailRequest.put("templateType", "PATIENT_APPOINTMENT_CREATED");
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("appointmentId", appointmentId);
            emailRequest.put("templateVariables", variables);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailRequest, headers);
            
            String url = NOTIFICATION_SERVICE_URL + "/api/notifications/email";
            
            restTemplate.postForEntity(url, entity, Map.class);
            
            log.info("✅ Patient notification sent");
            
        } catch (Exception e) {
            log.error("❌ Failed to send patient notification: {}", e.getMessage());
        }
    }
}