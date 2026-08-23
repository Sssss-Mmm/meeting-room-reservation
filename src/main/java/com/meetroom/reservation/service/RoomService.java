package com.meetroom.reservation.service;

import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    // 관리자용: 비활성 포함 전체 회의실 목록 조회
    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    // 활성화된 회의실 전체 목록 조회 (예약 폼 셀렉트박스용)
    public List<Room> findActiveRooms() {
        return roomRepository.findByActiveTrue();
    }

    // 활성화된 회의실 목록을 페이지 단위로 조회 (회의실 목록 페이지, 한 페이지 9개)
    public Page<Room> findActiveRooms(int page) {
        return roomRepository.findByActiveTrue(PageRequest.of(page, 9));
    }

    // 요청 시간대에 예약 가능한 회의실 검색 (빈 방 찾기)
    public List<Room> findAvailableRooms(LocalDateTime start, LocalDateTime end, int attendees) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }
        return roomRepository.findAvailable(start, end, attendees);
    }

    // ID로 회의실 단건 조회 (없으면 예외)
    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의실입니다."));
    }

    // 예약 생성 전용 조회 — 같은 회의실 예약이 이 행에서 직렬화된다
    // ponytail: 회의실 단위 락. 한 회의실에 동시 예약이 몰려 대기가 길어지면 시간대 단위로 쪼갠다
    public Room findByIdForUpdate(Long id) {
        return roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의실입니다."));
    }

    // 회의실 생성
    @Transactional
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    // 회의실 활성/비활성 상태 토글 (비활성화 시 예약 폼에서 제외됨)
    @Transactional
    public void toggleActive(Long id) {
        Room room = findById(id);
        room.toggleActive();
    }

    // 승인 정책 토글 (승인 필요 ↔ 즉시 확정)
    @Transactional
    public void toggleNeedsApproval(Long id) {
        Room room = findById(id);
        room.toggleNeedsApproval();
    }
}
