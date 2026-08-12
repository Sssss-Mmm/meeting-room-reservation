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

    // 특정 사용자의 예약 목록을 조회
    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

    // PENDING 상태 예약을 페이징 조회
    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room WHERE r.status = :status ORDER BY r.startTime ASC",
           countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    Page<Reservation> findPendingWithDetails(@Param("status") ReservationStatus status, Pageable pageable);

    // 오늘 날짜 범위의 취소·반려 제외 활성 예약을 조회
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room " +
           "WHERE r.startTime BETWEEN :start AND :end " +
           "AND r.status NOT IN (com.meetroom.reservation.domain.enums.ReservationStatus.CANCELLED, com.meetroom.reservation.domain.enums.ReservationStatus.REJECTED) " +
           "ORDER BY r.startTime ASC")
    List<Reservation> findTodayActiveWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 사용자별 예약 상태별 건수 집계
    @Query("SELECT r.status, COUNT(r) FROM Reservation r WHERE r.user.id = :userId GROUP BY r.status")
    List<Object[]> countGroupByStatus(@Param("userId") Long userId);

    // 동일 회의실·시간대 겹치는 예약을 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.status IN ('PENDING', 'CONFIRMED') AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservationsWithLock(@Param("roomId") Long roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 특정 회의실의 하루치 PENDING·CONFIRMED 예약 목록 조회 
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.status IN ('PENDING', 'CONFIRMED') AND r.startTime >= :startOfDay AND r.startTime < :endOfDay")
    List<Reservation> findByRoomIdAndDate(@Param("roomId") Long roomId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // 해당 월의 사용자 활성 예약을 조회 
    @Query("SELECT r FROM Reservation r JOIN FETCH r.room " +
           "WHERE r.user.id = :userId AND r.startTime BETWEEN :start AND :end " +
           "AND r.status NOT IN (com.meetroom.reservation.domain.enums.ReservationStatus.CANCELLED, com.meetroom.reservation.domain.enums.ReservationStatus.REJECTED)")
    List<Reservation> findActiveByUserIdAndMonth(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
