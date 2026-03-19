package com.kicktime.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Team team;

    @ManyToOne
    private Tournament tournament;

    private int played;

    private int wins;

    private int draws;

    private int losses;

    private int goalsFor;

    private int goalsAgainst;

    private int points;
}