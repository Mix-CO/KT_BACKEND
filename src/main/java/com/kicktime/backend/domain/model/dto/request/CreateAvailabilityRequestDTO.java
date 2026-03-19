package com.kicktime.backend.domain.model.dto.request;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAvailabilityRequestDTO {
    private Long userId;
    private Long timeSlotId;
}
