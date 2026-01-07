package com.premiereflow.premiereflowbackend.dto;

import lombok.Data;

@Data
public class SeatDto {
    private Long id;
    private int rowNum;
    private int seatNum;
    private String status;
    private double price;
}