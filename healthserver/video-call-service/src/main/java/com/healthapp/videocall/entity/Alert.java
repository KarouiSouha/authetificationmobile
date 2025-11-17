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

/**
 * Alert Entity - Alerts détectées par la caméra
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {
    
    @Id
    private String id;
    
    @Indexed
    private String deviceId;        // ID du téléphone/device
    
    @Indexed
    private String patientId;       // ID du patient (User)
    
    private String patientEmail;
    private String patientName;
    
    @Indexed
    private String alertType;       // FALL_DETECTION, ABNORMAL_MOVEMENT, etc.
    
    private Double confidence;      // Confiance du ML (0.0 - 1.0)
    
    private String metadata;        // JSON avec détails (image URL, etc.)
    
    @Builder.Default
    private String status = "PENDING";  // PENDING, PROCESSED, IGNORED
    
    private String appointmentId;   // ID du RDV créé suite à cette alerte
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;
}