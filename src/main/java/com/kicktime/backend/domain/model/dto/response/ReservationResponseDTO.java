package com.kicktime.backend.domain.model.dto.response;

import com.kicktime.backend.domain.model.enums.ReservationStatus;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDTO {

    private Long id;

    private Long matchId;

    private Long timeSlotId;

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private Long proposedByUserId;

    private String proposedByName;

    private ReservationStatus status;

    private LocalDateTime createdAt;
}