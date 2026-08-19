package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.Reservation;
import com.meetroom.reservation.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // PENDING 상태 예약을 페이징 조회
    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room WHERE r.status = :status ORDER BY r.startTime ASC",
           countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    Page<Reservation> findPendingWithDetails(@Param("status") ReservationStatus status, Pageable pageable);

    // 오늘 날짜 범위(start 이상 ~ end 미만)의 활성 예약을 조회
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room " +
           "WHERE r.status IN ('PENDING', 'CONFIRMED') AND r.startTime >= :start AND r.startTime < :end " +
           "ORDER BY r.startTime ASC")
    List<Reservation> findTodayActiveWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 사용자별 예약 상태별 건수 집계
    @Query("SELECT r.status, COUNT(r) FROM Reservation r WHERE r.user.id = :userId GROUP BY r.status")
    List<Object[]> countGroupByStatus(@Param("userId") Long userId);

    // 동일 회의실·시간대 겹치는 예약을 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.status IN ('PENDING', 'CONFIRMED') AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservationsWithLock(@Param("roomId") Long roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 해당 월의 사용자 활성 예약을 조회 (start 이상 ~ end 미만)
    @Query("SELECT r FROM Reservation r JOIN FETCH r.room " +
           "WHERE r.user.id = :userId AND r.status IN ('PENDING', 'CONFIRMED') " +
           "AND r.startTime >= :start AND r.startTime < :end")
    List<Reservation> findActiveByUserIdAndMonth(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
