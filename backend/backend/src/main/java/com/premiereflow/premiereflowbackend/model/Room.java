package com.premiereflow.premiereflowbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // e.g., "Hall 1"

    // One Room has many Seats (Physical layout)
    // "mappedBy" tells Hibernate that the "room" field in the Seat class owns the relationship
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<Seat> seats;
}