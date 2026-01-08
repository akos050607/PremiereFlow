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

        System.out.println("Reserve came: Seat " + event.getSeatId());

        Screening screening = screeningRepository.findById(event.getScreeningId()).orElseThrow();
        Seat seat = seatRepository.findById(event.getSeatId()).orElseThrow();

        Optional<Ticket> existingTicket = ticketRepository.findByScreeningIdAndSeatId(event.getScreeningId(), event.getSeatId());

        if (existingTicket.isPresent()) {
            if (existingTicket.get().getStatus() == SeatStatus.RESERVED) {
                return event;
            }
            Ticket ticket = existingTicket.get();
            ticket.setStatus(SeatStatus.valueOf(event.getStatus()));
            ticketRepository.save(ticket);
        } else {
            Ticket newTicket = new Ticket();
            newTicket.setScreening(screening);
            newTicket.setSeat(seat);
            newTicket.setStatus(SeatStatus.valueOf(event.getStatus()));
            ticketRepository.save(newTicket);
        }
        return event;
    }
}