package com.premiereflow.premiereflowbackend.service;

import com.premiereflow.premiereflowbackend.dto.ScreeningDetailsDto;
import com.premiereflow.premiereflowbackend.dto.SeatDto;
import com.premiereflow.premiereflowbackend.model.*;
import com.premiereflow.premiereflowbackend.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    public ScreeningService(ScreeningRepository screeningRepository, SeatRepository seatRepository, TicketRepository ticketRepository) {
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
    }

    public ScreeningDetailsDto getScreeningDetails(Long screeningId) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        List<Seat> allSeats = seatRepository.findByRoomId(screening.getRoom().getId());

        List<Ticket> soldTickets = ticketRepository.findByScreeningId(screeningId);

        // --- The Logic part: ---
        Map<Long, Ticket> ticketMap = soldTickets.stream()
                .collect(Collectors.toMap(ticket -> ticket.getSeat().getId(), ticket -> ticket));

        List<SeatDto> seatDtos = new ArrayList<>();

        for (Seat seat : allSeats) {
            SeatDto dto = new SeatDto();
            dto.setId(seat.getId());
            dto.setRowNum(seat.getRowNum());
            dto.setSeatNum(seat.getSeatNum());

            dto.setPrice(2000 * seat.getPriceModifier());

            if (ticketMap.containsKey(seat.getId())) {
                dto.setStatus(ticketMap.get(seat.getId()).getStatus().name());
            } else {
                dto.setStatus("FREE");
            }

            seatDtos.add(dto);
        }

        ScreeningDetailsDto response = new ScreeningDetailsDto();
        response.setId(screening.getId());
        response.setMovieTitle(screening.getMovie().getTitle());
        response.setRoomName(screening.getRoom().getName());
        response.setStartTime(screening.getStartTime().toString());
        response.setSeats(seatDtos);

        return response;
    }
}