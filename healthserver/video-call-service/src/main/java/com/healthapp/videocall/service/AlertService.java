package com.healthapp.videocall.service;

import com.healthapp.videocall.dto.AlertRequest;
import com.healthapp.videocall.entity.Alert;
import com.healthapp.videocall.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AlertService - Traitement des alertes caméra
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AlertService {
    
    private final AlertRepository alertRepository;
    private final AppointmentCreationService appointmentService;
    private final NotificationService notificationService;
    
    /**
     * Créer une alerte et déclencher le workflow
     */
    public Alert createAlert(AlertRequest request, String patientId, String patientEmail, String patientName) {
        log.info("🚨 New alert received: type={}, patientId={}, confidence={}", 
                request.getAlertType(), patientId, request.getConfidence());
        
        // Sauvegarder l'alerte
        Alert alert = Alert.builder()
                .deviceId(request.getDeviceId())
                .patientId(patientId)
                .patientEmail(patientEmail)
                .patientName(patientName)
                .alertType(request.getAlertType())
                .confidence(request.getConfidence())
                .metadata(request.getMetadata())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        
        Alert savedAlert = alertRepository.save(alert);
        
        // Décider si on crée un RDV urgent
        if (shouldTriggerAppointment(request)) {
            log.info("✅ Alert severity HIGH - Creating urgent appointment");
            
            String appointmentId = appointmentService.createUrgentAppointment(
                patientId, patientEmail, patientName, 
                "Alerte caméra: " + request.getAlertType()
            );
            
            savedAlert.setAppointmentId(appointmentId);
            savedAlert.setStatus("PROCESSED");
            savedAlert.setProcessedAt(LocalDateTime.now());
            alertRepository.save(savedAlert);
            
            // Notifier le médecin
            notificationService.notifyDoctorOfUrgentAppointment(appointmentId);
        } else {
            log.info("ℹ️ Alert severity LOW - Logged for review");
        }
        
        return savedAlert;
    }
    
    /**
     * Déterminer si l'alerte nécessite un RDV urgent
     */
    private boolean shouldTriggerAppointment(AlertRequest request) {
        // Logique métier : seuils de confiance, types d'alertes critiques, etc.
        if ("FALL_DETECTION".equals(request.getAlertType()) && request.getConfidence() > 0.8) {
            return true;
        }
        if ("ABNORMAL_BREATHING".equals(request.getAlertType()) && request.getConfidence() > 0.7) {
            return true;
        }
        return false;
    }
}