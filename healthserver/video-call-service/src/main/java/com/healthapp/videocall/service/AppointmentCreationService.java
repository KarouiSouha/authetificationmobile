package com.healthapp.videocall.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AppointmentCreationService - Créer des RDV urgents via Doctor Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentCreationService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // URL du doctor service (à adapter selon ton environnement)
    private static final String DOCTOR_SERVICE_URL = "http://localhost:8083";
    
    /**
     * Créer un RDV urgent suite à une alerte
     */
    public String createUrgentAppointment(String patientId, String patientEmail, 
                                         String patientName, String reason) {
        log.info("🚨 Creating URGENT appointment for patient: {}", patientEmail);
        
        try {
            // TODO: Récupérer le docteur de garde (pour l'instant, simulation)
            String doctorId = findAvailableDoctor();
            
            // Préparer la requête
            Map<String, Object> appointmentRequest = new HashMap<>();
            appointmentRequest.put("patientId", patientId);
            appointmentRequest.put("patientEmail", patientEmail);
            appointmentRequest.put("patientName", patientName);
            appointmentRequest.put("doctorId", doctorId);
            appointmentRequest.put("appointmentDateTime", LocalDateTime.now().plusMinutes(5).toString());
            appointmentRequest.put("appointmentType", "EMERGENCY");
            appointmentRequest.put("reason", "🚨 URGENT: " + reason);
            appointmentRequest.put("notes", "Rendez-vous créé automatiquement suite à alerte caméra");
            
            // Appeler l'API du doctor service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(appointmentRequest, headers);
            
            String url = DOCTOR_SERVICE_URL + "/api/doctors/appointments/from-patient";
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String appointmentId = (String) response.getBody().get("id");
                log.info("✅ Urgent appointment created: {}", appointmentId);
                return appointmentId;
            }
            
            throw new RuntimeException("Failed to create appointment: " + response.getStatusCode());
            
        } catch (Exception e) {
            log.error("❌ Failed to create urgent appointment: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create urgent appointment", e);
        }
    }
    
    /**
     * Trouver un docteur disponible
     * TODO: Implémenter logique de sélection (garde, spécialité, disponibilité)
     */
    private String findAvailableDoctor() {
        // Pour l'instant, retourner un ID fixe
        // En production: interroger doctor-service pour trouver un médecin de garde
        return "doctor_on_call_123";
    }
}