package com.premiereflow.premiereflowbackend.config;

import com.premiereflow.premiereflowbackend.model.*;
import com.premiereflow.premiereflowbackend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    // We inject the repositories to save data
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ScreeningRepository screeningRepository;
    private final TicketRepository ticketRepository;

    public DataSeeder(MovieRepository movieRepository,
                      RoomRepository roomRepository,
                      SeatRepository seatRepository,
                      ScreeningRepository screeningRepository,
                      TicketRepository ticketRepository) {
        this.movieRepository = movieRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.screeningRepository = screeningRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run if the database is empty
        if (movieRepository.count() == 0) {

            // 1. Create a Movie
            Movie avatar = new Movie();
            avatar.setTitle("Avatar: The Way of Water");
            avatar.setGenre("Sci-Fi");
            avatar.setDurationMin(192);
            avatar.setPosterUrl("https://example.com/avatar.jpg");
            movieRepository.save(avatar);

            // 2. Create a Room
            Room imaxRoom = new Room();
            imaxRoom.setName("IMAX Hall 1");
            roomRepository.save(imaxRoom);

            // 3. Create Seats for the Room (5 rows, 10 seats each = 50 seats)
            for (int row = 1; row <= 5; row++) {
                for (int seatNum = 1; seatNum <= 10; seatNum++) {
                    Seat seat = new Seat();
                    seat.setRoom(imaxRoom);
                    seat.setRowNum(row);
                    seat.setSeatNum(seatNum);
                    seat.setPriceModifier(1.0);
                    seatRepository.save(seat);
                }
            }

            // 4. Create a Screening (Tonight at 20:00)
            Screening screening = new Screening();
            screening.setMovie(avatar);
            screening.setRoom(imaxRoom);
            screening.setStartTime(LocalDateTime.now().withHour(20).withMinute(0));
            screeningRepository.save(screening);

            // 5. Simulate a booked ticket (Seat 1 in Row 1 is taken)
            // We need to fetch the seat first to link it
            Seat firstSeat = seatRepository.findAll().getFirst();

            Ticket ticket = new Ticket();
            ticket.setScreening(screening);
            ticket.setSeat(firstSeat);
            ticket.setStatus(SeatStatus.RESERVED);
            ticketRepository.save(ticket);

            System.out.println("--- DATA SEEDING COMPLETED ---");
        }
    }
}