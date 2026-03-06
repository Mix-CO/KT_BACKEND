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

    private LocalDateTime scheduledTime;

    @ManyToOne
    private Field field;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    private MatchResult result;
}