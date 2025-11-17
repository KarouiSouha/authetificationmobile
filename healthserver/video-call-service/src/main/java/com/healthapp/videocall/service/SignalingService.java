package com.healthapp.videocall.service;

import com.healthapp.videocall.dto.SignalingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * SignalingService - Gestion du signaling WebRTC
 * 
 * Alternative au broadcast automatique de @SendTo,
 * permet plus de contrôle sur l'envoi des messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignalingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Envoyer un message à tous les participants d'une session
     */
    public void broadcastToSession(String sessionId, String type, SignalingMessage message) {
        String destination = "/topic/signaling/" + sessionId + "/" + type;
        
        log.debug("📤 Broadcasting {} to {}", type, destination);
        
        messagingTemplate.convertAndSend(destination, message);
    }
    
    /**
     * Envoyer un message privé à un utilisateur spécifique
     */
    public void sendToUser(String userId, String type, SignalingMessage message) {
        String destination = "/queue/signaling/" + type;
        
        log.debug("📤 Sending {} to user {}", type, userId);
        
        messagingTemplate.convertAndSendToUser(userId, destination, message);
    }
    
    /**
     * Notifier qu'une session est prête
     */
    public void notifySessionReady(String sessionId, String userId) {
        SignalingMessage message = SignalingMessage.builder()
                .type("session_ready")
                .sessionId(sessionId)
                .build();
        
        sendToUser(userId, "session_ready", message);
    }
    
    /**
     * Notifier d'une erreur
     */
    public void notifyError(String sessionId, String errorMessage) {
        SignalingMessage message = SignalingMessage.builder()
                .type("error")
                .sessionId(sessionId)
                .build();
        
        broadcastToSession(sessionId, "error", message);
    }
}