package com.premiereflow.premiereflowbackend.controller;

import com.premiereflow.premiereflowbackend.dto.SeatEvent;
import com.premiereflow.premiereflowbackend.model.*;
import com.premiereflow.premiereflowbackend.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {

    private final TicketRepository ticketRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public WebSocketController(TicketRepository ticketRepository,
                               ScreeningRepository screeningRepository,
                               SeatRepository seatRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/lock-seat")
    @SendTo("/topic/seat-updates")
    @Transactional
    public SeatEvent handleSeatLock(@Payload SeatEvent event, SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        sessionUserMap.put(sessionId, event.getUserId());

        Screening screening = screeningRepository.findById(event.getScreeningId()).orElseThrow();
        Seat seat = seatRepository.findById(event.getSeatId()).orElseThrow();

        Optional<Ticket> existingTicket = ticketRepository.findByScreeningIdAndSeatId(event.getScreeningId(), event.getSeatId());

        if (existingTicket.isPresent()) {
            Ticket ticket = existingTicket.get();

            if (ticket.getStatus() == SeatStatus.RESERVED) {
                event.setStatus("RESERVED");
                return event;
            }

            // Ha LOCKED
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
        newTicket.setStatus(SeatStatus.valueOf(event.getStatus())); // LOCKED
        newTicket.setUserId(event.getUserId());
        ticketRepository.save(newTicket);

        return event;
    }

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sessionUserMap.get(sessionId);

        if (userId != null) {
            System.out.println("User disconnected: " + userId);

            List<Ticket> lockedTickets = ticketRepository.findAll().stream()
                    .filter(t -> t.getUserId() != null && t.getUserId().equals(userId))
                    .filter(t -> t.getStatus() == SeatStatus.LOCKED)
                    .toList();

            for (Ticket ticket : lockedTickets) {
                Seat seat = ticket.getSeat();

                ticketRepository.delete(ticket);

                SeatEvent freeEvent = new SeatEvent();
                freeEvent.setSeatId(seat.getId());
                freeEvent.setStatus("FREE");
                freeEvent.setUserId(null);

                messagingTemplate.convertAndSend("/topic/seat-updates", freeEvent);
            }

            sessionUserMap.remove(sessionId);
        }
    }

    @MessageMapping("/hover-seat")
    @SendTo("/topic/seat-hover")
    public SeatEvent handleHover(@Payload SeatEvent event) {
        return event;
    }

    @MessageMapping("/reset")
    @SendTo("/topic/seat-updates")
    @Transactional
    public SeatEvent handleReset(@Payload SeatEvent event) {
        List<Ticket> tickets = ticketRepository.findAll();
        ticketRepository.deleteAll(tickets);
        event.setStatus("RESET_ALL");
        event.setSeatId(-1L);
        return event;
    }

    @MessageMapping("/buy")
    @Transactional
    public void handleBuy(@Payload SeatEvent event) {
        List<Ticket> userTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getUserId() != null && t.getUserId().equals(event.getUserId()))
                .filter(t -> t.getStatus() == SeatStatus.LOCKED)
                .toList();

        for (Ticket ticket : userTickets) {
            ticket.setStatus(SeatStatus.RESERVED);
            ticketRepository.save(ticket);

            SeatEvent updateEvent = new SeatEvent();
            updateEvent.setSeatId(ticket.getSeat().getId());
            updateEvent.setStatus("RESERVED");
            updateEvent.setUserId(event.getUserId());

            messagingTemplate.convertAndSend("/topic/seat-updates", updateEvent);
        }

        SeatEvent successEvent = new SeatEvent();
        successEvent.setStatus("PURCHASE_SUCCESS");
        successEvent.setUserId(event.getUserId());
        messagingTemplate.convertAndSend("/topic/seat-updates", successEvent);
    }
}