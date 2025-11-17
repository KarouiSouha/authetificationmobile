package com.healthapp.videocall.controller;

import com.healthapp.videocall.dto.SignalingMessage;
import com.healthapp.videocall.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * SignalingController - Gestion des messages WebRTC via WebSocket
 * 
 * Flow:
 * 1. Patient envoie OFFER → relayé au Doctor
 * 2. Doctor envoie ANSWER → relayé au Patient
 * 3. Les deux envoient ICE candidates → relayés
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SignalingController {
    
    private final CallSessionService callSessionService;
    
    /**
     * Recevoir et relayer l'offre SDP
     * 
     * Client envoie à: /app/signaling/{sessionId}/offer
     * Broadcast vers: /topic/signaling/{sessionId}/offer
     */
    @MessageMapping("/signaling/{sessionId}/offer")
    @SendTo("/topic/signaling/{sessionId}/offer")
    public SignalingMessage handleOffer(@DestinationVariable String sessionId, 
                                        SignalingMessage message) {
        log.info("📤 Received OFFER for session: {}", sessionId);
        
        // Sauvegarder l'offre en BDD (optionnel, pour audit)
        callSessionService.saveOffer(sessionId, message.getSdp());
        
        // Relayer au pair (WebSocket broadcast automatique via @SendTo)
        return message;
    }
    
    /**
     * Recevoir et relayer la réponse SDP
     */
    @MessageMapping("/signaling/{sessionId}/answer")
    @SendTo("/topic/signaling/{sessionId}/answer")
    public SignalingMessage handleAnswer(@DestinationVariable String sessionId, 
                                         SignalingMessage message) {
        log.info("📤 Received ANSWER for session: {}", sessionId);
        
        // Sauvegarder la réponse
        callSessionService.saveAnswer(sessionId, message.getSdp());
        
        return message;
    }
    
    /**
     * Recevoir et relayer les ICE candidates
     */
    @MessageMapping("/signaling/{sessionId}/ice")
    @SendTo("/topic/signaling/{sessionId}/ice")
    public SignalingMessage handleIceCandidate(@DestinationVariable String sessionId, 
                                               SignalingMessage message) {
        log.debug("🧊 Received ICE candidate for session: {}", sessionId);
        
        // Pas besoin de sauvegarder les ICE (trop nombreux), juste relayer
        return message;
    }
    
    /**
     * Notification de déconnexion
     */
    @MessageMapping("/signaling/{sessionId}/leave")
    @SendTo("/topic/signaling/{sessionId}/leave")
    public SignalingMessage handleLeave(@DestinationVariable String sessionId, 
                                        SignalingMessage message) {
        log.info("👋 User leaving session: {}", sessionId);
        
        callSessionService.endSession(sessionId, "USER_LEFT");
        
        return message;
    }
}