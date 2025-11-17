package com.healthapp.videocall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration pour le signaling WebRTC
 * 
 * Endpoints:
 * - ws://localhost:8086/ws-signaling (connexion)
 * - /app/offer, /app/answer, /app/ice (envoi)
 * - /topic/signaling/{sessionId} (réception)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker simple en mémoire (pour dev/test)
        // En prod, utiliser RabbitMQ ou Redis
        config.enableSimpleBroker("/topic", "/queue");
        
        // Préfixe pour les messages envoyés par le client
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket avec SockJS fallback
        registry.addEndpoint("/ws-signaling")
                .setAllowedOriginPatterns("*")  // En prod: spécifier les origines
                .withSockJS();
    }
}