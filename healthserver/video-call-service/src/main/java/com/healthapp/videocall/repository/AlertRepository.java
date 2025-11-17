package com.healthapp.videocall.repository;

import com.healthapp.videocall.entity.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {
    
    // Trouver les alertes d'un patient
    List<Alert> findByPatientIdOrderByCreatedAtDesc(String patientId);
    
    // Trouver les alertes par type
    List<Alert> findByAlertType(String alertType);
    
    // Trouver les alertes en attente
    List<Alert> findByStatus(String status);
    
    // Trouver les alertes entre 2 dates
    List<Alert> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Compter les alertes d'un patient
    long countByPatientId(String patientId);
    
    // Alertes non traitées d'un patient
    List<Alert> findByPatientIdAndStatus(String patientId, String status);
}