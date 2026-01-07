package com.premiereflow.premiereflowbackend.repository;

import com.premiereflow.premiereflowbackend.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}