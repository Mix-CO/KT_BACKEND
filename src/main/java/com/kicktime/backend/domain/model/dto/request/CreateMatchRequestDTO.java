package com.kicktime.backend.domain.model.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMatchRequestDTO {

    private Long homeTeamId;

    private Long awayTeamId;

    private Long tournamentId;
}
