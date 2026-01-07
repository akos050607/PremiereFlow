package com.premiereflow.premiereflowbackend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which screening is this for?
    @ManyToOne
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    // Which physical seat is reserved?
    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // Status: LOCKED (yellow), RESERVED (red/sold)
    @Enumerated(EnumType.STRING)
    private SeatStatus status;
}