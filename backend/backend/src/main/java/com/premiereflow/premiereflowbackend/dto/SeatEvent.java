package com.premiereflow.premiereflowbackend.dto;

import lombok.Data;

@Data
public class SeatEvent {
    private Long screeningId;
    private String userId;
    private Long seatId;
    private String status; // LOCKED, RESERVED, FREE
    private String userSessionId; // To identify who locked it (optional for now)
}