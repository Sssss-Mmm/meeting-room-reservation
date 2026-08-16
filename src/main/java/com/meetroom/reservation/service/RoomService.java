package com.meetroom.reservation.service;

import com.meetroom.reservation.domain.Room;
import com.meetroom.reservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    // ID로 회의실 단건 조회 (없으면 예외)
    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의실입니다."));
    }

    // 회의실 생성
    @Transactional
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    // 회의실 정보 수정 (이름, 위치, 정원, 설명)
    @Transactional
    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = findById(id);
        room.update(updatedRoom.getName(), updatedRoom.getLocation(), updatedRoom.getCapacity(), updatedRoom.getDescription());
        return room;
    }

    // 회의실 활성/비활성 상태 토글 (비활성화 시 예약 폼에서 제외됨)
    @Transactional
    public void toggleActive(Long id) {
        Room room = findById(id);
        room.toggleActive();
    }
}
