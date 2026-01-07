package com.premiereflow.premiereflowbackend.repository;

import com.premiereflow.premiereflowbackend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Get all sold/locked tickets for a specific screening
    List<Ticket> findByScreeningId(Long screeningId);

    // Check if a specific seat is already taken for a screening
    Optional<Ticket> findByScreeningIdAndSeatId(Long screeningId, Long seatId);
}