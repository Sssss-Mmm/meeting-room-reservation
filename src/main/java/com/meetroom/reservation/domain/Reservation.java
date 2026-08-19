package com.meetroom.reservation.domain;

import com.meetroom.reservation.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class Reservation {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer attendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    private String rejectReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void confirm() {
        requirePending("승인");
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.REJECTED) {
            throw new IllegalStateException("이미 종료된 예약은 취소할 수 없습니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void reject(String reason) {
        requirePending("반려");
        this.status = ReservationStatus.REJECTED;
        this.rejectReason = reason;
    }

    // 상태 전이 규칙을 도메인에 모아둔다 — 서비스의 모든 호출부가 이 한 곳을 거친다
    private void requirePending(String action) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태의 예약만 " + action + "할 수 있습니다.");
        }
    }
}
