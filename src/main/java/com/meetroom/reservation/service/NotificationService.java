package com.meetroom.reservation.service;

import com.meetroom.reservation.domain.Notification;
import com.meetroom.reservation.domain.User;
import com.meetroom.reservation.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 대상 사용자에게 알림 메시지 생성 및 저장
    @Transactional
    public void create(User user, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .message(message)
                .build());
    }

    // 해당 사용자의 읽지 않은 알림 목록을 최신순으로 반환
    public List<Notification> findUnread(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    // 특정 알림을 읽음 처리 (본인 알림인지 확인 후 처리)
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.markRead();
            }
        });
    }

    // 해당 사용자의 모든 미읽음 알림을 일괄 읽음 처리
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
