package com.everspin.reservation.service;

import com.everspin.reservation.domain.Room;
import com.everspin.reservation.domain.User;
import com.everspin.reservation.domain.enums.Role;
import com.everspin.reservation.repository.ReservationRepository;
import com.everspin.reservation.repository.RoomRepository;
import com.everspin.reservation.repository.UserRepository;
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

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
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
                    reservationService.createReservation(user, roomId, "Meeting", "Discuss Something", start, end, 5);
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
}
