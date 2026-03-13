package com.kicktime.backend.domain.model.dto.response;

import com.kicktime.backend.domain.model.TimeSlot;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponseDTO {

    private Long id;

    private Long userId;

    private String userName;

    private TimeSlot timeSlot;
}