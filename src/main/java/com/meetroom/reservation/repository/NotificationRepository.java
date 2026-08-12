package com.meetroom.reservation.repository;

import com.meetroom.reservation.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 해당 사용자의 읽지 않은 알림을 최신순으로 조회
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    // 해당 사용자의 읽지 않은 알림 개수 반환
    long countByUserIdAndReadFalse(Long userId);

    // 해당 사용자의 미읽음 알림 전체를 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllReadByUserId(Long userId);
}
