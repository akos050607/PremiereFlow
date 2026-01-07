package com.premiereflow.premiereflowbackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rowNum;
    private int seatNum;

    private double priceModifier; // VIP status is a physical property of the seat

    // Link to the physical Room
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
}