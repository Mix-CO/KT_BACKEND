package com.kicktime.backend.domain.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSuggestionResponseDTO {
    private Long matchId;
    private String homeTeamName;
    private String awayTeamName;
    private Long suggestedTimeSlotId;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String explanation;
}