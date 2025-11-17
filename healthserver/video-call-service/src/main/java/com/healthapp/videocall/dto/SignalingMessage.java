package com.healthapp.videocall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SignalingMessage - Format des messages WebSocket pour signaling WebRTC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {
    
    private String type;       // offer, answer, ice, leave
    private String sessionId;
    private String senderId;   // ID de l'émetteur
    private String targetId;   // ID du destinataire (optionnel)
    
    // WebRTC SDP
    private String sdp;
    
    // ICE Candidate
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
    
    // Metadata
    private Long timestamp;
}