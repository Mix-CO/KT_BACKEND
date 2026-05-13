package com.kicktime.backend.domain.model.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleMatchRequestDTO {

    private Long timeSlotId;

    private Long fieldId;
}
