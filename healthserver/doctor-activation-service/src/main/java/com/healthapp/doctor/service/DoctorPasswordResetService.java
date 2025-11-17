package com.healthapp.doctor.service;

import com.healthapp.doctor.entity.Doctor;
import com.healthapp.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour gérer la réinitialisation des mots de passe DOCTOR
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorPasswordResetService {
    
    private final DoctorRepository doctorRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${notification.service.url:http://localhost:8084}")
    private String notificationServiceUrl;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Envoyer email de réinitialisation pour DOCTOR
     */
    public void sendPasswordResetEmailForDoctor(String email) {
        log.info("📧 Sending password reset email to DOCTOR: {}", email);
        
        // Vérifier si le doctor existe
        Doctor doctor = doctorRepository.findByEmail(email).orElse(null);
        
        if (doctor == null) {
            log.warn("⚠️ Doctor not found: {}", email);
            // ⚠️ Ne pas révéler que le doctor n'existe pas (sécurité)
            return;
        }
        
        // Générer un token de réinitialisation
        String resetToken = UUID.randomUUID().toString();
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken + "&type=doctor";
        
        
        // ✅ IMPORTANT: Envoyer à l'email de CONTACT, pas l'email système
        String recipientEmail = doctor.getNotificationEmail();
        
        log.info("📧 Sending to contact email: {}", recipientEmail);
        
        // Préparer les variables du template
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", doctor.getFullName());
        templateVariables.put("resetLink", resetLink);
        templateVariables.put("validityHours", "1");
        templateVariables.put("loginEmail", doctor.getEmail()); // Rappeler l'email de connexion
        
        // Préparer la requête email
        Map<String, Object> emailRequest = new HashMap<>();
        emailRequest.put("to", recipientEmail); // ✅ Contact email
        emailRequest.put("subject", "Password Reset Request - Health App Doctor");
        emailRequest.put("templateType", "PASSWORD_RESET");
        emailRequest.put("templateVariables", templateVariables);
        
        // Envoyer la requête au Notification Service
        try {
            String url = notificationServiceUrl + "/api/notifications/email";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                url, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.ACCEPTED) {
                log.info("✅ Password reset email sent to: {}", recipientEmail);
            } else {
                log.error("❌ Failed to send email. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("❌ Exception while sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}