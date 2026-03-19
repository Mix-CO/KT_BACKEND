package com.kicktime.backend.domain.model;

import com.kicktime.backend.domain.model.enums.TournamentCategory;
import com.kicktime.backend.domain.model.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String semester;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    @OneToMany(mappedBy = "tournament")
    private List<Team> teams;

    @OneToMany
    private List<Match> matches;

    private TournamentCategory category;
    private LocalDate startDate;
    private LocalDate endDate;
    private int minPlayersPerTeam;
    private int maxPlayersPerTeam;
    private int minTeams;
    private int maxTeams;
}