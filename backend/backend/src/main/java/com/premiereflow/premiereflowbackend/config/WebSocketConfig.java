package com.premiereflow.premiereflowbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages back to the client on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");

        // Designate the prefix for messages that are bound for @MessageMapping-annotated methods in application code
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the "/ws" endpoint, enabling SockJS fallback options so that alternate transports can be used if WebSocket is not available.
        // The SockJS client will attempt to connect to "http://localhost:8080/ws"
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173") // Allow React Frontend
                .withSockJS();
    }
}