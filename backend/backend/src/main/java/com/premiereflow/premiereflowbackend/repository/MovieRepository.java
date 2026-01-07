package com.premiereflow.premiereflowbackend.repository;

import com.premiereflow.premiereflowbackend.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

// We extend JpaRepository to get free CRUD operations (Save, Delete, FindAll)
public interface MovieRepository extends JpaRepository<Movie, Long> {
}