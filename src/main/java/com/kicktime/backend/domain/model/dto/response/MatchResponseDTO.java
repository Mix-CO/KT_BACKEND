package com.kicktime.backend.domain.model.dto.response;

import com.kicktime.backend.domain.model.enums.MatchStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDTO {

    private Long id;

    private Long homeTeamId;

    private String homeTeamName;

    private Long awayTeamId;

    private String awayTeamName;

    private MatchStatus status;

    private Long fieldId;

    private Long timeSlotId;

    private Integer homeScore;

    private Integer awayScore;
}
