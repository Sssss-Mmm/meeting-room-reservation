package com.meetroom.reservation.service;

import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.Role;
import com.meetroom.reservation.dto.ReservationRequest;
import com.meetroom.reservation.repository.ReservationRepository;
import com.meetroom.reservation.repository.RoomRepository;
import com.meetroom.reservation.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;

    private Long roomId;
    private List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Room room = Room.builder()
                .name("Concurrency Room")
                .location("1F")
                .capacity(10)
                .active(true)
                .build();
        roomId = roomRepository.save(room).getId();

        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .username("user" + i)
                    .password("pass")
                    .name("User " + i)
                    .email("user" + i + "@test.com")
                    .role(Role.ROLE_USER)
                    .build();
            userIds.add(userRepository.save(user).getId());
        }
    }

    @Test
    void testConcurrentReservation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    User user = userRepository.findById(userIds.get(index)).orElseThrow();
                    reservationService.createReservation(user, request(roomId, start, end));
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        
        long count = reservationRepository.count();
        assertThat(count).isEqualTo(1); // 동시성 제어로 인해 1개만 성공해야 함
    }

    private ReservationRequest request(Long roomId, LocalDateTime start, LocalDateTime end) {
        ReservationRequest r = new ReservationRequest();
        r.setRoomId(roomId);
        r.setTitle("Meeting");
        r.setDescription("Discuss Something");
        r.setStartTime(start);
        r.setEndTime(end);
        r.setAttendees(5);
        return r;
    }
}
