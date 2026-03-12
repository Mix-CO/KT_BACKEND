package com.kicktime.backend.domain.model.dto.request;

import com.kicktime.backend.domain.model.enums.TournamentCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTournamentRequestDTO {

    private String name;

    private String semester;

    private TournamentCategory category;

    private LocalDate startDate;

    private LocalDate endDate;

    private int minPlayersPerTeam;

    private int maxPlayersPerTeam;

    private int minTeams;

    private int maxTeams;
}
