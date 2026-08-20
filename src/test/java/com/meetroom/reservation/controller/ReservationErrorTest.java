package com.meetroom.reservation.controller;

import com.meetroom.reservation.domain.Reservation;
import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.ReservationStatus;
import com.meetroom.reservation.domain.enums.Role;
import com.meetroom.reservation.repository.ReservationRepository;
import com.meetroom.reservation.repository.RoomRepository;
import com.meetroom.reservation.repository.UserRepository;
import com.meetroom.reservation.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 화면에서 거부된 요청이 500이 아니라 목록 + 에러 메시지로 돌아오는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationErrorTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;

    private CustomUserDetails owner;
    private CustomUserDetails stranger;
    private Long startedId;

    @BeforeEach
    void setUp() {
        User ownerUser = userRepository.save(newUser("owner"));
        owner = new CustomUserDetails(ownerUser);
        stranger = new CustomUserDetails(userRepository.save(newUser("stranger")));

        Room room = roomRepository.save(Room.builder()
                .name("회의실A").location("1F").capacity(10).build());

        // 이미 시작된 예약 — 취소가 거부되어야 한다
        startedId = reservationRepository.save(Reservation.builder()
                .user(ownerUser).room(room)
                .title("회의").attendees(5)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .status(ReservationStatus.CONFIRMED)
                .build()).getId();
    }

    @Test
    void 존재하지_않는_예약을_취소하면_목록으로_돌아간다() throws Exception {
        mockMvc.perform(post("/reservations/{id}/cancel", 999999L)
                        .with(user(owner)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 존재하지_않는_예약을_조회하면_목록으로_돌아간다() throws Exception {
        mockMvc.perform(get("/reservations/{id}", 999999L).with(user(owner)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"));
    }

    @Test
    void 이미_시작된_예약은_취소할_수_없고_에러_메시지가_남는다() throws Exception {
        mockMvc.perform(post("/reservations/{id}/cancel", startedId)
                        .with(user(owner)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 남의_예약은_취소할_수_없다() throws Exception {
        mockMvc.perform(post("/reservations/{id}/cancel", startedId)
                        .with(user(stranger)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    private User newUser(String username) {
        return User.builder()
                .username(username).password("pw").name(username)
                .email(username + "@test.com").role(Role.ROLE_USER)
                .build();
    }
}
