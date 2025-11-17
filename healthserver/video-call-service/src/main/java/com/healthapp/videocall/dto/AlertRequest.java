package com.healthapp.videocall.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "Alert type is required")
    private String alertType;  // FALL_DETECTION, ABNORMAL_MOVEMENT, etc.
    
    @NotNull(message = "Confidence is required")
    @DecimalMin(value = "0.0", message = "Confidence must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence must be <= 1")
    private Double confidence;
    
    private String metadata;  // JSON optionnel avec détails
}