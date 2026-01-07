package com.premiereflow.premiereflowbackend.repository;

import com.premiereflow.premiereflowbackend.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    // Find all screenings for a specific movie
    List<Screening> findByMovieId(Long movieId);
}