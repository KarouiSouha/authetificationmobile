package com.healthapp.videocall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Video Call Service
 * 
 * Gestion des alertes caméra et sessions d'appel vidéo WebRTC
 * 
 * Endpoints:
 * - POST /api/alerts - Créer une alerte
 * - POST /api/calls/create - Créer une session d'appel
 * - WebSocket: ws://localhost:8086/ws-signaling
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableMongoAuditing
public class VideoCallServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoCallServiceApplication.class, args);
        
        System.out.println("""
            
            ========================================
            📹 Video Call Service démarré!
            📍 Port: 8086
            📍 Endpoints:
               POST /api/alerts
               POST /api/calls/create
               POST /api/calls/{id}/end
               WebSocket: /ws-signaling
            ========================================
            """);
    }
}