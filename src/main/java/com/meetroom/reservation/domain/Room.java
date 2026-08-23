package com.meetroom.reservation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(nullable = false)
    private Integer capacity;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // true면 관리자 승인 후 확정, false면 예약 즉시 확정
    @Column(nullable = false)
    @Builder.Default
    private boolean needsApproval = false;

    public void toggleActive() {
        this.active = !this.active;
    }

    public void toggleNeedsApproval() {
        this.needsApproval = !this.needsApproval;
    }
}
