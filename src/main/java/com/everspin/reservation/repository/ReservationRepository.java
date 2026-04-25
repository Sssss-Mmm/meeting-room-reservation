package com.everspin.reservation.repository;

import com.everspin.reservation.domain.Reservation;
import com.everspin.reservation.domain.enums.ReservationStatus;
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

    List<Reservation> findByUserIdOrderByStartTimeDesc(Long userId);

    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room WHERE r.status = :status ORDER BY r.startTime ASC",
           countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    Page<Reservation> findPendingWithDetails(@Param("status") ReservationStatus status, Pageable pageable);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room " +
           "WHERE r.startTime BETWEEN :start AND :end " +
           "AND r.status NOT IN (com.everspin.reservation.domain.enums.ReservationStatus.CANCELLED, com.everspin.reservation.domain.enums.ReservationStatus.REJECTED) " +
           "ORDER BY r.startTime ASC")
    List<Reservation> findTodayActiveWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r.status, COUNT(r) FROM Reservation r WHERE r.user.id = :userId GROUP BY r.status")
    List<Object[]> countGroupByStatus(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.status IN ('PENDING', 'CONFIRMED') AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservationsWithLock(@Param("roomId") Long roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId AND r.status IN ('PENDING', 'CONFIRMED') AND r.startTime >= :startOfDay AND r.startTime < :endOfDay")
    List<Reservation> findByRoomIdAndDate(@Param("roomId") Long roomId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.room " +
           "WHERE r.user.id = :userId AND r.startTime BETWEEN :start AND :end " +
           "AND r.status NOT IN (com.everspin.reservation.domain.enums.ReservationStatus.CANCELLED, com.everspin.reservation.domain.enums.ReservationStatus.REJECTED)")
    List<Reservation> findActiveByUserIdAndMonth(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
