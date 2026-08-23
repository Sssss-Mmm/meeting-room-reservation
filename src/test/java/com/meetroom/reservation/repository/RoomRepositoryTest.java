package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.Reservation;
import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.ReservationStatus;
import com.meetroom.reservation.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 빈 회의실 검색 쿼리 검증 — 로직이 JPQL 안에 있으므로 H2로 실제 실행해서 확인한다.
 */
@DataJpaTest
class RoomRepositoryTest {

    @Autowired private RoomRepository roomRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 1, 14, 0);
    private static final LocalDateTime END = START.plusHours(1);

    @Test
    void 요청_시간대에_겹치는_예약이_없는_활성_회의실만_반환한다() {
        Room free = room("빈방", 10, true);
        Room booked = room("찬방", 10, true);
        room("폐쇄방", 10, false);
        reserve(booked, START.plusMinutes(30), END.plusMinutes(30), ReservationStatus.CONFIRMED);

        List<Room> available = roomRepository.findAvailable(START, END, 1);

        assertThat(available).extracting(Room::getName).containsExactly(free.getName());
    }

    @Test
    void 정원이_부족한_회의실은_제외하고_작은_방부터_반환한다() {
        room("4인실", 4, true);
        room("8인실", 8, true);
        room("20인실", 20, true);

        List<Room> available = roomRepository.findAvailable(START, END, 6);

        // 6명이 들어가는 방만, 그중 작은 방 우선 (큰 방을 낭비하지 않는다)
        assertThat(available).extracting(Room::getName).containsExactly("8인실", "20인실");
    }

    @Test
    void 취소_반려된_예약은_방을_점유하지_않는다() {
        Room room = room("취소된방", 10, true);
        reserve(room, START, END, ReservationStatus.CANCELLED);

        assertThat(roomRepository.findAvailable(START, END, 1)).hasSize(1);
    }

    @Test
    void 끝나는_시각과_시작_시각이_맞닿는_예약은_겹침이_아니다() {
        Room room = room("연달아방", 10, true);
        reserve(room, START.minusHours(1), START, ReservationStatus.CONFIRMED);

        assertThat(roomRepository.findAvailable(START, END, 1)).hasSize(1);
    }

    private Room room(String name, int capacity, boolean active) {
        return roomRepository.save(Room.builder()
                .name(name).location("1F").capacity(capacity).active(active).build());
    }

    private void reserve(Room room, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        User user = userRepository.save(User.builder()
                .username("u" + System.nanoTime()).password("pw").name("사용자")
                .email("u" + System.nanoTime() + "@test.com").role(Role.ROLE_USER).build());
        reservationRepository.save(Reservation.builder()
                .user(user).room(room).title("회의").attendees(1)
                .startTime(start).endTime(end).status(status).build());
    }
}
