package com.healthapp.user.service;

import com.healthapp.user.entity.User;
import com.healthapp.user.repository.UserRepository;
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
 * Service pour gérer la réinitialisation des mots de passe
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${notification.service.url:http://localhost:8084}")
    private String notificationServiceUrl;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Envoyer email de réinitialisation pour USER
     */
    public void sendPasswordResetEmailForUser(String email) {
        log.info("📧 Sending password reset email to USER: {}", email);
        
        // Vérifier si l'utilisateur existe
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            log.warn("⚠️ User not found: {}", email);
            // ⚠️ Ne pas révéler que l'user n'existe pas (sécurité)
            return;
        }
        
        // Générer un token de réinitialisation
        String resetToken = UUID.randomUUID().toString();
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        // TODO: Sauvegarder le token dans la DB avec expiration
        // (Pour l'instant, on envoie juste l'email)
        
        // Préparer les variables du template
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("userName", user.getFullName());
        templateVariables.put("resetLink", resetLink);
        templateVariables.put("validityHours", "1");
        
        // Préparer la requête email
        Map<String, Object> emailRequest = new HashMap<>();
        emailRequest.put("to", email);
        emailRequest.put("subject", "Password Reset Request - Health App");
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
                log.info("✅ Password reset email sent to: {}", email);
            } else {
                log.error("❌ Failed to send email. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("❌ Exception while sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}