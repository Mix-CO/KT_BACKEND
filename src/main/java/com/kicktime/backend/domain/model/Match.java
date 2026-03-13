package com.kicktime.backend.domain.model;

import com.kicktime.backend.domain.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Team homeTeam;

    @ManyToOne
    private Team awayTeam;

    @ManyToOne
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    private MatchResult result;

    @ManyToOne
    private Tournament tournament;

    @OneToOne
    private Field field;

}