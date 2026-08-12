package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // 활성화된 회의실 전체 목록 조회
    List<Room> findByActiveTrue();

    // 활성화된 회의실 목록을 페이지 단위로 조회
    Page<Room> findByActiveTrue(Pageable pageable);
}
