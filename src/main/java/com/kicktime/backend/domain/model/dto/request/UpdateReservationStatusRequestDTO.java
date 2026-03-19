package com.kicktime.backend.domain.model.dto.request;

import com.kicktime.backend.domain.model.enums.ReservationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReservationStatusRequestDTO {

    private ReservationStatus status;
}
