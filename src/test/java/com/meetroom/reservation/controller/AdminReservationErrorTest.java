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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 도메인의 상태 전이 거부(IllegalStateException)가 관리자 화면에서 에러 메시지로 보이는지 검증한다.
 * 500 페이지로 새는 순간 관리자는 "왜 안 되는지"를 알 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminReservationErrorTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;

    private CustomUserDetails admin;
    private Long confirmedId;

    @BeforeEach
    void setUp() {
        admin = new CustomUserDetails(User.builder()
                .username("admin-test").password("pw").name("관리자")
                .email("admin-test@test.com").role(Role.ROLE_ADMIN)
                .build());

        User owner = userRepository.save(User.builder()
                .username("owner").password("pw").name("예약자")
                .email("owner@test.com").role(Role.ROLE_USER)
                .build());
        Room room = roomRepository.save(Room.builder()
                .name("회의실A").location("1F").capacity(10).build());

        confirmedId = reservationRepository.save(Reservation.builder()
                .user(owner).room(room)
                .title("회의").attendees(5)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(ReservationStatus.CONFIRMED)
                .build()).getId();
    }

    @Test
    void 이미_승인된_예약을_다시_승인하면_에러_메시지와_함께_대시보드로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/reservations/{id}/approve", confirmedId)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 이미_승인된_예약을_반려하면_에러_메시지와_함께_대시보드로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/reservations/{id}/reject", confirmedId)
                        .with(user(admin)).with(csrf())
                        .param("reason", "일정 변경"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 존재하지_않는_예약을_승인하면_에러_메시지와_함께_대시보드로_돌아간다() throws Exception {
        mockMvc.perform(post("/admin/reservations/{id}/approve", 999999L)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
