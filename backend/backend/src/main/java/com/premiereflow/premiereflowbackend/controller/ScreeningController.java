package com.premiereflow.premiereflowbackend.controller;

import com.premiereflow.premiereflowbackend.dto.ScreeningDetailsDto;
import com.premiereflow.premiereflowbackend.service.ScreeningService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/screenings")
@CrossOrigin(origins = "http://localhost:5173") // We enable this for React frontend
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    // GET http://localhost:8080/api/screenings/1
    @GetMapping("/{id}")
    public ScreeningDetailsDto getScreening(@PathVariable Long id) {
        return screeningService.getScreeningDetails(id);
    }
}