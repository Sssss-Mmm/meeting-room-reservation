package com.everspin.reservation.service;

import com.everspin.reservation.domain.Reservation;
import com.everspin.reservation.domain.Room;
import com.everspin.reservation.domain.User;
import com.everspin.reservation.domain.enums.ReservationStatus;
import com.everspin.reservation.domain.enums.Role;
import com.everspin.reservation.dto.ReservationRequest;
import com.everspin.reservation.repository.ReservationRepository;
import com.everspin.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final List<ReservationStatus> INACTIVE_STATUSES =
            List.of(ReservationStatus.CANCELLED, ReservationStatus.REJECTED);

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public List<Reservation> findByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    public Page<Reservation> findPendingReservations(int page) {
        return reservationRepository.findPendingWithDetails(
                ReservationStatus.PENDING, PageRequest.of(page, 10));
    }

    public List<Reservation> findTodayReservations() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return reservationRepository.findTodayActiveWithDetails(startOfDay, endOfDay, INACTIVE_STATUSES);
    }

    public Map<Integer, List<Reservation>> findMonthlyCalendar(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end   = ym.atEndOfMonth().atTime(23, 59, 59);
        return reservationRepository.findActiveByUserIdAndMonth(userId, start, end, INACTIVE_STATUSES).stream()
                .collect(Collectors.groupingBy(r -> r.getStartTime().getDayOfMonth()));
    }

    public Map<String, Long> countByStatus(Long userId) {
        return reservationRepository.countGroupByStatus(userId).stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]));
    }

    @Transactional
    public Reservation createReservation(User user, ReservationRequest request) {
        Room room = roomService.findById(request.getRoomId());
        if (!room.isActive()) {
            throw new IllegalArgumentException("비활성화된 회의실입니다.");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        if (request.getAttendees() > room.getCapacity()) {
            throw new IllegalArgumentException("참석 인원이 회의실 정원을 초과합니다.");
        }

        // 동시성 제어 - 비관적 락 사용
        List<Reservation> overlapping = reservationRepository.findOverlappingReservationsWithLock(
                request.getRoomId(), request.getStartTime(), request.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .room(room)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .attendees(request.getAttendees())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // 예약자에게 알림
        notificationService.create(user,
                String.format("[%s] '%s' 예약 신청이 접수되었습니다. 관리자 승인 후 확정됩니다.",
                        room.getName(), request.getTitle()));

        // 관리자 전원에게 알림
        userRepository.findByRole(Role.ROLE_ADMIN).forEach(admin ->
                notificationService.create(admin,
                        String.format("[신규 예약] %s님이 '%s(%s)' 예약을 신청했습니다.",
                                user.getName(), request.getTitle(), room.getName())));

        return saved;
    }

    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation res = findById(id);
        if (!res.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
        }
        if (res.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("이미 시작된 예약은 취소할 수 없습니다.");
        }
        res.cancel();
        notificationService.create(res.getUser(),
                String.format("[%s] '%s' 예약이 취소되었습니다.", res.getRoom().getName(), res.getTitle()));
    }

    @Transactional
    public void approveReservation(Long id) {
        Reservation res = findById(id);
        res.confirm();
        notificationService.create(res.getUser(),
                String.format("[%s] '%s' 예약이 승인되었습니다.", res.getRoom().getName(), res.getTitle()));
    }

    @Transactional
    public void rejectReservation(Long id, String reason) {
        Reservation res = findById(id);
        res.reject(reason);
        notificationService.create(res.getUser(),
                String.format("[%s] '%s' 예약이 반려되었습니다. 사유: %s",
                        res.getRoom().getName(), res.getTitle(), reason));
    }
}
