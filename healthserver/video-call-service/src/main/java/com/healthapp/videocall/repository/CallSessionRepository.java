package com.healthapp.videocall.repository;

import com.healthapp.videocall.entity.CallSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends MongoRepository<CallSession, String> {
    
    // Trouver par appointmentId
    Optional<CallSession> findByAppointmentId(String appointmentId);
    
    // Sessions d'un patient
    List<CallSession> findByPatientIdOrderByCreatedAtDesc(String patientId);
    
    // Sessions d'un docteur
    List<CallSession> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    
    // Sessions par statut
    List<CallSession> findByStatus(String status);
    
    // Sessions expirées (pour cleanup)
    List<CallSession> findByExpiresAtBefore(LocalDateTime now);
    
    // Dernière session entre 2 utilisateurs
    Optional<CallSession> findTopByPatientIdAndDoctorIdOrderByCreatedAtDesc(
        String patientId, String doctorId
    );
    
    // Compter les sessions complétées d'un docteur
    long countByDoctorIdAndStatus(String doctorId, String status);
}