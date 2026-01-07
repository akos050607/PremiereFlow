package com.premiereflow.premiereflowbackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ScreeningDetailsDto {
    private Long id;
    private String movieTitle;
    private String roomName;
    private String startTime;
    private List<SeatDto> seats;
}