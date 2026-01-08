package com.premiereflow.premiereflowbackend.controller;

import com.premiereflow.premiereflowbackend.dto.SeatEvent;
import com.premiereflow.premiereflowbackend.model.*;
import com.premiereflow.premiereflowbackend.repository.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Controller
public class WebSocketController {

    private final TicketRepository ticketRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;

    public WebSocketController(TicketRepository ticketRepository, ScreeningRepository screeningRepository, SeatRepository seatRepository) {
        this.ticketRepository = ticketRepository;
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
    }

    @MessageMapping("/lock-seat")
    @SendTo("/topic/seat-updates")
    @Transactional
    public SeatEvent handleSeatLock(@Payload SeatEvent event) {
        Screening screening = screeningRepository.findById(event.getScreeningId()).orElseThrow();
        Seat seat = seatRepository.findById(event.getSeatId()).orElseThrow();

        Optional<Ticket> existingTicket = ticketRepository.findByScreeningIdAndSeatId(event.getScreeningId(), event.getSeatId());

        if (existingTicket.isPresent()) {
            Ticket ticket = existingTicket.get();

            if (ticket.getStatus() == SeatStatus.RESERVED) {
                event.setStatus("RESERVED");
                return event;
            }

            if (ticket.getStatus() == SeatStatus.LOCKED) {
                if (ticket.getUserId() != null && ticket.getUserId().equals(event.getUserId())) {
                    ticketRepository.delete(ticket);
                    event.setStatus("FREE");
                    event.setUserId(null);
                    return event;
                } else {
                    event.setStatus("LOCKED");
                    return event;
                }
            }
        }

        Ticket newTicket = new Ticket();
        newTicket.setScreening(screening);
        newTicket.setSeat(seat);
        newTicket.setStatus(SeatStatus.valueOf(event.getStatus()));
        newTicket.setUserId(event.getUserId());
        ticketRepository.save(newTicket);

        return event;
    }

    @MessageMapping("/hover-seat")
    @SendTo("/topic/seat-hover")
    public SeatEvent handleHover(@Payload SeatEvent event) {
        return event;
    }
}