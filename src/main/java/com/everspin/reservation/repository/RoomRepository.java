package com.everspin.reservation.repository;

import com.everspin.reservation.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrue();
    Page<Room> findByActiveTrue(Pageable pageable);
}
