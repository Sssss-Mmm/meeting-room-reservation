package com.meetroom.reservation.dto;

import com.meetroom.reservation.domain.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationDto {
    private final Long id;
    private final String message;
    private final LocalDateTime createdAt;

    public NotificationDto(Notification n) {
        this.id = n.getId();
        this.message = n.getMessage();
        this.createdAt = n.getCreatedAt();
    }
}
