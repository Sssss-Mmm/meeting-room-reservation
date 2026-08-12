package com.meetroom.reservation.service;

import com.meetroom.reservation.domain.Reservation;
import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.domain.enums.ReservationStatus;
import com.meetroom.reservation.domain.enums.Role;
import com.meetroom.reservation.dto.ReservationRequest;
import com.meetroom.reservation.repository.ReservationRepository;
import com.meetroom.reservation.repository.UserRepository;
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

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 특정 사용자의 전체 예약 목록을 최신순으로 반환
    public List<Reservation> findByUserId(Long userId) {
        return reservationRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    // ID로 예약 단건 조회 (없으면 예외)
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));
    }

    // 관리자용: 승인 대기(PENDING) 예약 목록을 페이지 단위로 조회
    public Page<Reservation> findPendingReservations(int page) {
        return reservationRepository.findPendingWithDetails(
                ReservationStatus.PENDING, PageRequest.of(page, 10));
    }

    // 오늘 날짜의 활성 예약 전체 조회 (대시보드 오늘 예약 현황 표시용)
    public List<Reservation> findTodayReservations() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        return reservationRepository.findTodayActiveWithDetails(startOfDay, endOfDay);
    }

    // 해당 월의 사용자 활성 예약을 일자(day)별로 그룹핑하여 반환 — 월별 달력 뷰 구성용
    public Map<Integer, List<Reservation>> findMonthlyCalendar(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end   = ym.atEndOfMonth().atTime(23, 59, 59);
        return reservationRepository.findActiveByUserIdAndMonth(userId, start, end).stream()
                .collect(Collectors.groupingBy(r -> r.getStartTime().getDayOfMonth()));
    }

    // 사용자의 상태별 예약 건수를 Map<상태명, 건수>로 반환 (대시보드 통계 카드용)
    public Map<String, Long> countByStatus(Long userId) {
        return reservationRepository.countGroupByStatus(userId).stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]));
    }

    // 예약 생성: 유효성 검증 → 비관적 락으로 중복 예약 방지 → 저장 → 예약자·관리자 알림 발송
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

    // 예약 취소: 본인 예약 여부 및 시작 전 예약인지 확인 후 취소 처리 및 알림 발송
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

    // 관리자: 예약 승인 처리 및 예약자에게 알림 발송
    @Transactional
    public void approveReservation(Long id) {
        Reservation res = findById(id);
        res.confirm();
        notificationService.create(res.getUser(),
                String.format("[%s] '%s' 예약이 승인되었습니다.", res.getRoom().getName(), res.getTitle()));
    }

    // 관리자: 사유와 함께 예약 반려 처리 및 예약자에게 알림 발송
    @Transactional
    public void rejectReservation(Long id, String reason) {
        Reservation res = findById(id);
        res.reject(reason);
        notificationService.create(res.getUser(),
                String.format("[%s] '%s' 예약이 반려되었습니다. 사유: %s",
                        res.getRoom().getName(), res.getTitle(), reason));
    }
}
