package com.healthapp.doctor.service;

import com.healthapp.doctor.dto.response.CallSessionResponse;
import com.healthapp.doctor.entity.Appointment;
import com.healthapp.doctor.entity.CallSession;
import com.healthapp.doctor.repository.AppointmentRepository;
import com.healthapp.doctor.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebRTC Service - Gestion des sessions d'appel avec Metered.ca
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebRTCService {
    private final CallSessionRepository callSessionRepository;
    private final AppointmentRepository appointmentRepository;
    private final MeteredTurnCredentialsGenerator credentialsGenerator;

    /**
     * Initier un appel (DOCTOR ou PATIENT)
     */
    public CallSessionResponse initiateCall(String appointmentId, String callType, String initiatorEmail) {
        log.info("📞 Initiating {} call for appointment: {}", callType, appointmentId);

        // Vérifier que le rendez-vous existe
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Déterminer le rôle de l'initiateur
        boolean isDoctorInitiating = appointment.getDoctorEmail().equals(initiatorEmail);
        String initiatorRole = isDoctorInitiating ? "DOCTOR" : "PATIENT";

        // Générer des credentials TURN temporaires (expiration 24h)
        String iceServersJson = credentialsGenerator.generateIceServersJson();

        // Créer la session d'appel avec ICE servers Metered.ca
        CallSession session = CallSession.builder()
                .appointmentId(appointmentId)
                .doctorId(appointment.getDoctorId())
                .doctorEmail(appointment.getDoctorEmail())
                .patientId(appointment.getPatientId())
                .patientEmail(appointment.getPatientEmail())
                .callType(callType)
                .status("INITIATED")
                .initiatorRole(initiatorRole)
                .iceServers(iceServersJson)
                .createdAt(LocalDateTime.now())
                .build();

        CallSession saved = callSessionRepository.save(session);

        log.info("✅ Call session created: {} (Initiator: {}) with Metered.ca TURN servers (expires in 24h)", 
                 saved.getId(), initiatorRole);

        // TODO: Envoyer notification push au destinataire
        // if (isDoctorInitiating) {
        //     notificationService.notifyPatient(appointment.getPatientId(), "Incoming call from Dr. " + appointment.getDoctorName());
        // } else {
        //     notificationService.notifyDoctor(appointment.getDoctorId(), "Incoming call from " + appointment.getPatientName());
        // }

        return mapToResponse(saved);
    }

    /**
     * Sauvegarder l'offre SDP
     */
    public void saveOfferSdp(String callId, String sdp) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.setOfferSdp(sdp);
        session.setStatus("RINGING");
        callSessionRepository.save(session);

        log.info("📤 Offer SDP saved for call: {}", callId);
    }

    /**
     * Sauvegarder l'answer SDP
     */
    public void saveAnswerSdp(String callId, String sdp) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.setAnswerSdp(sdp);
        callSessionRepository.save(session);

        log.info("📥 Answer SDP saved for call: {}", callId);
    }

    /**
     * Marquer l'appel comme actif (connexion WebRTC établie)
     */
    public void markCallAsActive(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.markAsActive();
        callSessionRepository.save(session);

        log.info("✅ Call {} is now ACTIVE", callId);
    }

    /**
     * Terminer l'appel
     */
    public void endCall(String callId, String reason) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        session.end(reason);
        callSessionRepository.save(session);

        log.info("📵 Call {} ended: {} (Duration: {}s)",
                callId, reason, session.getDurationSeconds());
    }

    /**
     * Récupérer une session d'appel
     */
    public CallSessionResponse getCallSession(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        return mapToResponse(session);
    }

    /**
     * Obtenir les métriques de qualité d'appel (QoS)
     */
    public Map<String, Object> getCallQuality(String callId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call session not found"));

        Map<String, Object> qos = new HashMap<>();
        qos.put("callId", session.getId());
        qos.put("durationSeconds", session.getDurationSeconds());
        qos.put("status", session.getStatus());
        qos.put("endReason", session.getEndReason());
        qos.put("networkType", session.getNetworkType());
        qos.put("callType", session.getCallType());

        return qos;
    }

    /**
     * Mapper vers DTO
     */
    private CallSessionResponse mapToResponse(CallSession session) {
        return CallSessionResponse.builder()
                .callId(session.getId())
                .appointmentId(session.getAppointmentId())
                .doctorId(session.getDoctorId())
                .doctorEmail(session.getDoctorEmail())
                .patientId(session.getPatientId())
                .patientEmail(session.getPatientEmail())
                .callType(session.getCallType())
                .status(session.getStatus())
                .initiatorRole(session.getInitiatorRole())
                .iceServers(session.getIceServers())
                .offerSdp(session.getOfferSdp())
                .answerSdp(session.getAnswerSdp())
                .createdAt(session.getCreatedAt())
                .build();
    }
}