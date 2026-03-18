package com.kicktime.backend.domain.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponseDTO {

    private Long id;
    private Long userId;
    private String userName;
    private Long timeSlotId;
}