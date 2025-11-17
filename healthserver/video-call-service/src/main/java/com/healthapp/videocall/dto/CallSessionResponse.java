package com.healthapp.videocall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSessionResponse {
    
    private String callId;
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String status;
    
    // ICE Servers configuration
    private List<Map<String, Object>> iceServers;
    
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    // Optional: Signaling endpoint
    private String signalingUrl;
}