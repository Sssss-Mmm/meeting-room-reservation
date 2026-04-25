package com.everspin.reservation.service;

import com.everspin.reservation.domain.Room;
import com.everspin.reservation.repository.RoomRepository;
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

    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> findActiveRooms() {
        return roomRepository.findByActiveTrue();
    }

    public Page<Room> findActiveRooms(int page) {
        return roomRepository.findByActiveTrue(PageRequest.of(page, 9));
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의실입니다."));
    }

    @Transactional
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = findById(id);
        room.update(updatedRoom.getName(), updatedRoom.getLocation(), updatedRoom.getCapacity(), updatedRoom.getDescription());
        return room;
    }

    @Transactional
    public void toggleActive(Long id) {
        Room room = findById(id);
        room.toggleActive();
    }
}
