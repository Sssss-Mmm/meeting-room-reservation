package com.everspin.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationRequest {

    @NotNull(message = "회의실을 선택해주세요.")
    private Long roomId;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String description;

    @NotNull(message = "시작 시간을 입력해주세요.")
    @Future(message = "과거 시간은 예약할 수 없습니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "종료 시간을 입력해주세요.")
    @Future(message = "과거 시간은 예약할 수 없습니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @NotNull(message = "참석 인원을 입력해주세요.")
    @Min(value = 1, message = "참석 인원은 1명 이상이어야 합니다.")
    private Integer attendees;
}
