package com.kicktime.backend.domain.model.dto.response;

import com.kicktime.backend.domain.model.enums.TournamentCategory;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentResponseDTO {

    private Long id;

    private String name;

    private String semester;

    private TournamentStatus status;

    private TournamentCategory category;

    private LocalDate startDate;

    private LocalDate endDate;

    private int minPlayersPerTeam;

    private int maxPlayersPerTeam;

    private int minTeams;

    private int maxTeams;

    private List<TeamResponseDTO> teams;
}
