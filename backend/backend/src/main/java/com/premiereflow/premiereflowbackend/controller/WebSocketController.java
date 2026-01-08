package com.premiereflow.premiereflowbackend.controller;

import com.premiereflow.premiereflowbackend.dto.SeatEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    // When frontend sends a message to "/app/lock-seat"
    @MessageMapping("/lock-seat")
    // Use the return value to broadcast to all subscribers of "/topic/seat-updates"
    @SendTo("/topic/seat-updates")
    public SeatEvent handleSeatLock(@Payload SeatEvent event) {

        System.out.println("Real-time event received: " + event.getSeatId() + " -> " + event.getStatus());

        // TODO: In the future, we will save this to the database/Redis here!
        // For now, we just echo the message back to everyone so they see the update immediately.

        return event;
    }
}