package com.kicktime.backend.domain.model.dto.response;

import com.kicktime.backend.domain.model.enums.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDTO {

    private Long id;

    private Long matchId;

    private Long timeSlotId;

    private Long proposedByUserId;

    private String proposedByName;

    private ReservationStatus status;

    private LocalDateTime createdAt;
}
