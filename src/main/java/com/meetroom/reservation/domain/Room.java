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

    public void toggleActive() {
        this.active = !this.active;
    }
}
