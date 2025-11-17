package com.healthapp.videocall.controller;

import com.healthapp.videocall.dto.CallSessionResponse;
import com.healthapp.videocall.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CallSessionController - Endpoints pour gérer les sessions d'appel
 */
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
@Slf4j
public class CallSessionController {
    
    private final CallSessionService callSessionService;
    
    /**
     * Créer une session d'appel pour un RDV
     * 
     * POST /api/calls/create
     * Body: { "appointmentId": "...", "patientId": "...", "doctorId": "..." }
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DOCTOR', 'USER')")
    public ResponseEntity<CallSessionResponse> createCallSession(@RequestBody Map<String, String> request) {
        
        String appointmentId = request.get("appointmentId");
        String patientId = request.get("patientId");
        String patientEmail = request.get("patientEmail");
        String doctorId = request.get("doctorId");
        String doctorEmail = request.get("doctorEmail");
        
        log.info("📞 Creating call session for appointment: {}", appointmentId);
        
        CallSessionResponse response = callSessionService.createCallSession(
            appointmentId, patientId, patientEmail, doctorId, doctorEmail
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupérer les détails d'une session
     * 
     * GET /api/calls/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<CallSessionResponse> getCallSession(@PathVariable String sessionId) {
        
        // TODO: Implémenter récupération session
        
        return ResponseEntity.ok(CallSessionResponse.builder()
            .callId(sessionId)
            .status("ACTIVE")
            .build());
    }
    
    /**
     * Terminer une session
     * 
     * POST /api/calls/{sessionId}/end
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Map<String, String>> endCallSession(@PathVariable String sessionId,
                                                               @RequestBody Map<String, String> request) {
        
        String endReason = request.getOrDefault("reason", "USER_ENDED");
        
        log.info("📞 Ending call session: {} - Reason: {}", sessionId, endReason);
        
        callSessionService.endSession(sessionId, endReason);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Call session ended"
        ));
    }
    
    /**
     * Récupérer les métadonnées d'une session terminée
     * 
     * GET /api/calls/{sessionId}/metadata
     */
    @GetMapping("/{sessionId}/metadata")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCallMetadata(@PathVariable String sessionId) {
        
        // TODO: Implémenter récupération métadonnées (durée, QoS, etc.)
        
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "duration", 300,
            "quality", "good"
        ));
    }
}