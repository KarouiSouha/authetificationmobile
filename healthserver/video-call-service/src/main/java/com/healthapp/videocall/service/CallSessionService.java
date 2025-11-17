package com.healthapp.videocall.service;

import com.healthapp.videocall.dto.CallSessionResponse;
import com.healthapp.videocall.entity.CallSession;
import com.healthapp.videocall.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CallSessionService - Gestion complète des sessions avec signaling HTTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallSessionService {
    
    private final CallSessionRepository callSessionRepository;
    
    // ✅ NOUVEAU: Cache en mémoire pour signaling (alternative à WebSocket)
    private final Map<String, Map<String, Object>> signalingCache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> iceCandidatesCache = new ConcurrentHashMap<>();
    
    @Value("${cloudflare.turn.url}")
    private String turnUrl;
    
    @Value("${cloudflare.turn.username}")
    private String turnUsername;
    
    @Value("${cloudflare.turn.credential}")
    private String turnCredential;
    
    /**
     * Créer une session d'appel
     */
    public CallSessionResponse createCallSession(String appointmentId, String patientId, 
                                                  String patientEmail, String doctorId, 
                                                  String doctorEmail) {
        log.info("📞 Creating call session for appointment: {}", appointmentId);
        
        // Configuration ICE servers
        List<Map<String, Object>> iceServers = List.of(
            Map.of("urls", List.of("stun:stun.l.google.com:19302")),
            Map.of("urls", List.of("stun:stun.cloudflare.com:3478")),
            Map.of(
                "urls", List.of(turnUrl),
                "username", turnUsername,
                "credential", turnCredential
            )
        );
        
        CallSession session = CallSession.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .patientEmail(patientEmail)
                .doctorId(doctorId)
                .doctorEmail(doctorEmail)
                .status("INITIATED")
                .iceServers(iceServers)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();
        
        CallSession saved = callSessionRepository.save(session);
        
        // Initialiser le cache signaling
        signalingCache.put(saved.getId(), new ConcurrentHashMap<>());
        iceCandidatesCache.put(saved.getId(), new ArrayList<>());
        
        return mapToResponse(saved);
    }
    
    /**
     * ✅ NOUVEAU: Récupérer une session
     */
    public CallSessionResponse getSession(String sessionId) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return mapToResponse(session);
    }
    
    /**
     * Enregistrer l'offre SDP
     */
    public void saveOffer(String sessionId, String offerSDP) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setOfferSDP(offerSDP);
        callSessionRepository.save(session);
        
        log.info("✅ Offer SDP saved for session: {}", sessionId);
    }
    
    /**
     * Enregistrer la réponse SDP
     */
    public void saveAnswer(String sessionId, String answerSDP) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setAnswerSDP(answerSDP);
        session.setStatus("CONNECTED");
        session.setConnectedAt(LocalDateTime.now());
        callSessionRepository.save(session);
        
        log.info("✅ Answer SDP saved for session: {}", sessionId);
    }
    
    /**
     * ✅ NOUVEAU: Stocker les données de signaling (offer/answer)
     */
    public void storeSignalingData(String sessionId, String type, Map<String, Object> data) {
        Map<String, Object> sessionCache = signalingCache.computeIfAbsent(
            sessionId, k -> new ConcurrentHashMap<>()
        );
        
        sessionCache.put(type, data);
        sessionCache.put(type + "_timestamp", System.currentTimeMillis());
        
        log.debug("📦 Stored {} for session: {}", type, sessionId);
    }
    
    /**
     * ✅ NOUVEAU: Récupérer les données de signaling
     */
    public Map<String, Object> getSignalingData(String sessionId, String type) {
        Map<String, Object> sessionCache = signalingCache.get(sessionId);
        
        if (sessionCache == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) sessionCache.get(type);
        
        // Retourner et effacer (consommation unique)
        if (data != null) {
            sessionCache.remove(type);
            log.debug("📤 Retrieved {} for session: {}", type, sessionId);
        }
        
        return data;
    }
    
    /**
     * ✅ NOUVEAU: Stocker un ICE candidate
     */
    public void storeIceCandidate(String sessionId, Map<String, Object> ice) {
        List<Map<String, Object>> candidates = iceCandidatesCache.computeIfAbsent(
            sessionId, k -> new ArrayList<>()
        );
        
        ice.put("timestamp", System.currentTimeMillis());
        candidates.add(ice);
        
        log.debug("🧊 Stored ICE candidate for session: {}", sessionId);
    }
    
    /**
     * ✅ NOUVEAU: Récupérer les ICE candidates
     */
    public Map<String, Object> getIceCandidates(String sessionId) {
        List<Map<String, Object>> candidates = iceCandidatesCache.get(sessionId);
        
        if (candidates == null || candidates.isEmpty()) {
            return Map.of("candidates", List.of());
        }
        
        // Retourner une copie et vider
        List<Map<String, Object>> result = new ArrayList<>(candidates);
        candidates.clear();
        
        log.debug("📤 Retrieved {} ICE candidates for session: {}", result.size(), sessionId);
        
        return Map.of("candidates", result);
    }
    
    /**
     * Terminer une session
     */
    public void endSession(String sessionId, String endReason) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setStatus("ENDED");
        session.setEndedAt(LocalDateTime.now());
        session.setEndReason(endReason);
        
        if (session.getConnectedAt() != null) {
            long duration = java.time.Duration.between(
                session.getConnectedAt(), 
                session.getEndedAt()
            ).getSeconds();
            session.setDurationSeconds((int) duration);
        }
        
        callSessionRepository.save(session);
        
        // Nettoyer le cache
        signalingCache.remove(sessionId);
        iceCandidatesCache.remove(sessionId);
        
        log.info("📞 Call session ended: {} - Duration: {}s", sessionId, session.getDurationSeconds());
    }
    
    /**
     * Mapper vers DTO Response
     */
    private CallSessionResponse mapToResponse(CallSession session) {
        return CallSessionResponse.builder()
                .callId(session.getId())
                .appointmentId(session.getAppointmentId())
                .patientId(session.getPatientId())
                .doctorId(session.getDoctorId())
                .status(session.getStatus())
                .iceServers(session.getIceServers())
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .signalingUrl("http://localhost:8087/api/public/calls/signaling/" + session.getId())
                .build();
    }
}