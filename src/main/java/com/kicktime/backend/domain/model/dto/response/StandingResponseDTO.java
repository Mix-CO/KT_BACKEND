package com.kicktime.backend.domain.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandingResponseDTO {

    private Long id;

    private Long teamId;

    private String teamName;

    private Long tournamentId;

    private int played;

    private int wins;

    private int draws;

    private int losses;

    private int goalsFor;

    private int goalsAgainst;

    private int points;
}