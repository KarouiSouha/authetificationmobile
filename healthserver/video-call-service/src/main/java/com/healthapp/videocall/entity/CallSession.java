package com.healthapp.videocall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CallSession - Session d'appel vidéo WebRTC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "call_sessions")
public class CallSession {
    
    @Id
    private String id;
    
    @Indexed
    private String appointmentId;   // Lien vers le RDV
    
    @Indexed
    private String patientId;
    private String patientEmail;
    
    @Indexed
    private String doctorId;
    private String doctorEmail;
    
    @Builder.Default
    private String status = "INITIATED";  // INITIATED, CONNECTED, ENDED, FAILED
    
    // ICE Servers configuration (STUN/TURN)
    private List<Map<String, Object>> iceServers;
    
    // WebRTC Signaling data
    private String offerSDP;        // SDP de l'offre
    private String answerSDP;       // SDP de la réponse
    
    // Call quality metrics
    private Integer durationSeconds;
    private String qualityMetrics;  // JSON avec packet loss, jitter, etc.
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime connectedAt;
    private LocalDateTime endedAt;
    
    private String endReason;       // USER_ENDED, TIMEOUT, ERROR
    
    // Expiration (pour cleanup automatique)
    private LocalDateTime expiresAt;
}