package com.kicktime.backend.domain.model;

import com.kicktime.backend.domain.model.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;

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
}