package com.kicktime.backend.domain.model.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequestDTO {

    private Long matchId;

    private Long timeSlotId;

    private Long userId;
}

