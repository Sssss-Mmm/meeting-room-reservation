package com.everspin.reservation.service;

import com.everspin.reservation.domain.Reservation;
import com.everspin.reservation.domain.Room;
import com.everspin.reservation.domain.User;
import com.everspin.reservation.domain.enums.ReservationStatus;
import com.everspin.reservation.domain.enums.Role;
import com.everspin.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spring 컨텍스트 없이 ReservationService 로직만 검증하는 단위 테스트.
 * 의존성(Repository, NotificationService)은 Mockito로 대체 — 실행이 1초 이내라 TDD 사이클을 빠르게 돌 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RoomService roomService;
    @Mock private NotificationService notificationService;
    @Mock private com.everspin.reservation.repository.UserRepository userRepository;

    @InjectMocks private ReservationService reservationService;

    @Test
    void 이미_취소된_예약은_다시_취소할_수_없다() {
        // given: 이미 CANCELLED 상태이고, 시작 시각은 아직 미래인 예약
        Reservation cancelled = reservation(ReservationStatus.CANCELLED);
        given(reservationRepository.findById(1L)).willReturn(Optional.of(cancelled));

        // when & then: 두 번째 취소는 거부되어야 한다
        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소");

        // 그리고 중복 알림도 발송되지 않아야 한다
        verify(notificationService, never()).create(any(), any());
    }

    @Test
    void 취소된_예약은_승인할_수_없다() {
        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation(ReservationStatus.CANCELLED)));

        assertThatThrownBy(() -> reservationService.approveReservation(1L))
                .isInstanceOf(IllegalStateException.class);

        verify(notificationService, never()).create(any(), any());
    }

    @Test
    void 이미_승인된_예약은_반려할_수_없다() {
        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation(ReservationStatus.CONFIRMED)));

        assertThatThrownBy(() -> reservationService.rejectReservation(1L, "일정 변경"))
                .isInstanceOf(IllegalStateException.class);

        verify(notificationService, never()).create(any(), any());
    }

    @Test
    void 대기중인_본인_예약은_정상적으로_취소된다() {
        Reservation pending = reservation(ReservationStatus.PENDING);
        given(reservationRepository.findById(1L)).willReturn(Optional.of(pending));

        reservationService.cancelReservation(1L, 10L);

        assertThat(pending.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(notificationService).create(any(), any());
    }

    private Reservation reservation(ReservationStatus status) {
        User owner = User.builder()
                .id(10L).username("user").password("pw")
                .name("사용자").email("user@test.com").role(Role.ROLE_USER)
                .build();
        Room room = Room.builder().id(1L).name("회의실A").location("1F").capacity(10).active(true).build();
        return Reservation.builder()
                .id(1L).user(owner).room(room)
                .title("회의").attendees(5)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(status)
                .build();
    }
}
