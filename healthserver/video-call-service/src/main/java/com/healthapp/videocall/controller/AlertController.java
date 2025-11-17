package com.healthapp.videocall.controller;

import com.healthapp.videocall.dto.AlertRequest;
import com.healthapp.videocall.entity.Alert;
import com.healthapp.videocall.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * AlertController - Endpoints pour gérer les alertes caméra
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {
    
    private final AlertService alertService;
    
    /**
     * Créer une nouvelle alerte (envoyée par l'app patient)
     * 
     * POST /api/alerts
     * Body: { "deviceId": "...", "alertType": "FALL_DETECTION", "confidence": 0.95 }
     */
    @PostMapping
    public ResponseEntity<Alert> createAlert(@Valid @RequestBody AlertRequest request,
                                             Authentication authentication) {
        
        // Récupérer les infos du patient authentifié
        String patientEmail = authentication.getName();
        
        // TODO: Récupérer patientId et patientName depuis User Service
        // Pour l'instant, simulation:
        String patientId = "patient_" + patientEmail.split("@")[0];
        String patientName = patientEmail.split("@")[0];
        
        log.info("🚨 Alert received from patient: {}", patientEmail);
        
        Alert alert = alertService.createAlert(request, patientId, patientEmail, patientName);
        
        return ResponseEntity.ok(alert);
    }
    
    /**
     * Récupérer l'historique des alertes du patient
     */
    @GetMapping("/history")
    public ResponseEntity<?> getAlertHistory(Authentication authentication) {
        String patientEmail = authentication.getName();
        
        // TODO: Implémenter récupération historique
        
        return ResponseEntity.ok("Alert history for: " + patientEmail);
    }
}