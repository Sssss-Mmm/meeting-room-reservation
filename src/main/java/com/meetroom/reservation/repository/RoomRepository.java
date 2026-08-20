package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // 활성화된 회의실 전체 목록 조회
    List<Room> findByActiveTrue();

    // 활성화된 회의실 목록을 페이지 단위로 조회
    Page<Room> findByActiveTrue(Pageable pageable);

    // 예약 생성 시 회의실 행을 잠근다 — 아직 없는 예약의 '빈틈'은 DB마다 잠기는 방식이 다르지만,
    // 반드시 존재하는 회의실 행은 어디서나 잠긴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
